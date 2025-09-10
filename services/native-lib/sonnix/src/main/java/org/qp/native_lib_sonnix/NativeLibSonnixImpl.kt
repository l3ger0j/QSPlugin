package org.qp.native_lib_sonnix

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat.fromUri
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.libsnxqsp.jni.SNXLib
import org.qp.dto.GameInterface
import org.qp.dto.LibGameState
import org.qp.dto.LibGenItem
import org.qp.dto.LibIProxy
import org.qp.dto.LibReturnValue
import org.qp.dto.LibTypeDialog
import org.qp.dto.LibTypePopup
import org.qp.dto.LibTypeWindow
import org.qp.utils.FileUtil.readFileContents
import org.qp.utils.FileUtil.isWritableDir
import org.qp.utils.FileUtil.isWritableFile
import org.qp.utils.FileUtil.writeFileContents
import org.qp.utils.HtmlUtil.getSrcDir
import org.qp.utils.HtmlUtil.isContainsHtmlTags
import org.qp.utils.PathUtil.getFilename
import org.qp.utils.PathUtil.normalizeContentPath
import org.qp.utils.ThreadUtil.isSameThread
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts

class NativeLibSonnixImpl(
    private val context: Context,
    override var gameInterface: GameInterface,
    override var gameState: LibGameState = LibGameState(),
    override val returnValueFlow: MutableSharedFlow<LibReturnValue> = MutableSharedFlow(),
) : SNXLib(), LibIProxy {

    private val libLock = ReentrantLock()
    private var libThread: Thread? = null
    private var libUIFlowJob: Job? = null
    private var libStateFlowJob: Job? = null
    private val libScope = CoroutineScope(Dispatchers.Default)
    private val libQueue: ArrayBlockingQueue<Runnable> = ArrayBlockingQueue(1)
    @Volatile private lateinit var libHandler: Handler
    @Volatile private var libThreadInit = false
    @Volatile private var gameStartTime: Long = 0L
    @Volatile private var lastMsCountCallTime: Long = 0L
    private val currGameDir: DocumentFile?
        get() = fromUri(context, gameState.gameDirUri)

    private val mutableMenuItemList: MutableList<LibGenItem> = mutableListOf()

    private fun runOnQspThread(runnable: Runnable) {
        if (libThreadInit) {
            libHandler.post {
                libLock.withLock { runnable.run() }
            }
        } else {
            libQueue.offer(runnable)
        }
    }

    private fun loadGameWorld(): Boolean {
        val gameFileUri = gameState.gameFileUri
        val gameFile = gameFileUri.toDocumentFile(context) ?: return false
        val gameFileFullPath = gameFile.getAbsolutePath(context)
        val gameData = gameFileUri.readFileContents(context) ?: return false

        if (!loadGameWorldFromData(gameData, gameFileFullPath)) {
            showLastQspError()
            return false
        }

        return true
    }

    private fun showLastQspError() {
        val errorData = lastErrorData
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_ERROR,
            inputString =
                """
                Location: ${errorData.locName ?: ""}
                Action: ${errorData.index}
                Line: ${errorData.line}
                Error number: ${errorData.errorNum}
                Description: ${getErrorDesc(errorData.errorNum) ?: ""}
                """.trimIndent()
        )
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     * @return `true` if the configuration has changed, otherwise `false`
     */
    private fun loadUIConfiguration() {
        val htmlResult = getVarValues("USEHTML", 0)
        val fSizeResult = getVarValues("FSIZE", 0)
        val bColorResult = getVarValues("BCOLOR", 0)
        val fColorResult = getVarValues("FCOLOR", 0)
        val lColorResult = getVarValues("LCOLOR", 0)

        val useHtml = htmlResult.intValue != 0
        val oldConfig = gameInterface.gameUIConfFlow.value

        libUIFlowJob = libScope.launch {
            gameInterface.gameUIConfFlow.emit(
                oldConfig.copy(
                    useHtml = if (htmlResult.isSuccess) useHtml else oldConfig.useHtml,
                    fontSize = if (fSizeResult.isSuccess) fSizeResult.intValue else oldConfig.fontSize,
                    backColor = if (bColorResult.isSuccess) bColorResult.intValue else oldConfig.backColor,
                    fontColor = if (fColorResult.isSuccess) fColorResult.intValue else oldConfig.fontColor,
                    linkColor = if (lColorResult.isSuccess) lColorResult.intValue else oldConfig.linkColor
                )
            )
        }
    }

    @OptIn(ExperimentalContracts::class)
    private val actionsList: List<LibGenItem>
        get() {
            val gameDir = currGameDir
            if (!gameDir.isWritableDir(context)) return emptyList()

            val actions = mutableListOf<LibGenItem>()
            for (element in getActions()) {
                if (element == null) continue
                var tempImagePath = element.image ?: ""
                val tempText = element.text ?: ""

                if (tempImagePath.isNotBlank()) {
                    val tempPath = tempImagePath.getFilename().normalizeContentPath()
                    val fileFromPath = gameDir.child(context, tempPath)
                    if (fileFromPath.isWritableFile(context)) {
                        tempImagePath = fileFromPath.uri.toString()
                    }
                }

                actions.add(LibGenItem(tempText, tempImagePath))
            }

            return actions
        }

    @OptIn(ExperimentalContracts::class)
    private val objectsList: List<LibGenItem>
        get() {
            val gameDir = currGameDir
            if (!gameDir.isWritableDir(context)) return emptyList()

            val objects = mutableListOf<LibGenItem>()
            for (element in getObjects()) {
                if (element == null) continue
                var tempImagePath = element.image ?: ""
                val tempText = element.text ?: ""

                if (tempText.contains("<img")) {
                    if (!tempText.isContainsHtmlTags()) {
                        val fileFromPath = gameDir.child(context, tempText)
                        if (fileFromPath.isWritableFile(context)) {
                            tempImagePath = fileFromPath.uri.toString()
                        }
                    } else {
                        val tempPath = tempText.getSrcDir()
                        val fileFromPath = gameDir.child(context, tempPath)
                        if (fileFromPath.isWritableFile(context)) {
                            tempImagePath = fileFromPath.uri.toString()
                        }
                    }
                }

                objects.add(LibGenItem(tempText, tempImagePath))
            }

            return objects
        }

    // region LibQpProxy
    override fun startLibThread() {
        libThread = thread(name = "libSNXQSP") {
            init()
            Looper.prepare()
            libHandler = Handler(Looper.myLooper()!!)
            libThreadInit = true
            while (!libQueue.isEmpty()) {
                libQueue.poll()?.run()
            }
            Looper.loop()
            terminate()
        }
    }

    override fun stopLibThread() {
        if (libThreadInit) {
            val handler = libHandler
            handler.looper.quitSafely()
            libThreadInit = false
        }

        libStateFlowJob?.cancel()
        libUIFlowJob?.cancel()

        libThread?.interrupt()
    }

    override fun enableDebugMode(isDebug: Boolean) {
        runOnQspThread { enableDebugMode(isDebug) }
    }

    override fun runGame(
        gameId: Long,
        gameTitle: String,
        gameDirUri: Uri,
        gameFileUri: Uri
    ) {
        runOnQspThread { doRunGame(gameId, gameTitle, gameDirUri, gameFileUri) }
    }

    override fun restartGame() {
        runOnQspThread {
            doRunGame(
                gameState.gameId,
                gameState.gameTitle,
                gameState.gameDirUri,
                gameState.gameFileUri
            )
        }
    }

    private fun doRunGame(id: Long, title: String, dir: Uri, file: Uri) {
        gameInterface.doWithCounterDisabled {
            gameInterface.closeAllFiles()
            gameState = gameState.copy(
                gameRunning = true,
                gameId = id,
                gameTitle = title,
                gameDirUri = dir,
                gameFileUri = file
            )
            if (!loadGameWorld()) return@doWithCounterDisabled
            gameStartTime = SystemClock.elapsedRealtime()
            lastMsCountCallTime = 0
            if (!restartGame(true)) {
                showLastQspError()
            }
        }
    }

    override fun loadGameState(uri: Uri) {
        if (!isSameThread(libHandler.looper.thread)) {
            runOnQspThread { loadGameState(uri) }
            return
        }

        val gameData = uri.readFileContents(context) ?: return
        if (!openSavedGameFromData(gameData, true)) {
            showLastQspError()
        }
    }

    override fun saveGameState(uri: Uri) {
        if (!isSameThread(libHandler.looper.thread)) {
            runOnQspThread { saveGameState(uri) }
            return
        }

        uri.writeFileContents(
            context = context,
            dataToWrite = saveGameAsData(false) ?: return
        )
    }

    override fun onActionClicked(index: Int) {
        runOnQspThread {
            if (!setSelActionIndex(index, false)) {
                showLastQspError()
            }
            if (!executeSelActionCode(true)) {
                showLastQspError()
            }
        }
    }

    override fun onObjectSelected(index: Int) {
        runOnQspThread {
            if (!setSelObjectIndex(index, true)) {
                showLastQspError()
            }
        }
    }

    override fun onInputAreaClicked(code: String) {
        runOnQspThread {
            setInputStrText(code)
            if (!execUserInput(true)) {
                showLastQspError()
            }
        }
    }

    override fun onUseExecutorString(code: String) {
        runOnQspThread {
            if (!execString(code, true)) {
                showLastQspError()
            }
        }
    }

    override fun execute(code: String?) {
        runOnQspThread {
            if (!execString(code, true)) {
                showLastQspError()
            }
        }
    }

    override fun executeCounter() {
        if (libLock.isLocked) return
        runOnQspThread {
            if (!execCounter(true)) {
                showLastQspError()
            }
        }
    }

    override fun onRefreshInt() {
        loadUIConfiguration()

        gameState = gameState.copy(
            mainDesc = mainDesc ?: "",
            varsDesc = varsDesc ?: "",
            actionsList = actionsList,
            objectsList = objectsList
        )

        libStateFlowJob = libScope.launch {
            gameInterface.gameStateFlow.emit(gameState)
        }
    }

    override fun onShowImage(path: String?) {
        if (!path.isNullOrBlank()) {
            gameInterface.showLibDialog(LibTypeDialog.DIALOG_PICTURE, path)
        }
    }

    override fun onSetTimer(msecs: Int) {
        gameInterface.setCountInter(msecs.toLong())
    }

    override fun onShowMessage(message: String?) {
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_MESSAGE,
            inputString = message ?: ""
        )
    }

    override fun onPlayFile(path: String?, volume: Int) {
        if (!path.isNullOrBlank()) {
            gameInterface.playFile(path, volume)
        }
    }

    override fun onIsPlayingFile(path: String?): Boolean {
        if (path.isNullOrBlank()) {
            return false
        } else {
            gameInterface.isPlayingFile(path)
            return try {
                runBlocking { returnValueFlow.first() }.playFileState
            } catch (_: Exception) {
                false
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onCloseFile(path: String?) {
        if (path.isNullOrBlank()) {
            gameInterface.closeAllFiles()
        } else {
            gameInterface.closeFile(path)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onOpenGame(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_LOAD)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestReceiveFile(filename)
                    runBlocking { returnValueFlow.first() }.fileUri
                }
                .thenAccept {
                    if (it != null && it != Uri.EMPTY) {
                        gameInterface.doWithCounterDisabled { loadGameState(it) }
                    } else {
                        gameInterface.showLibDialog(
                            LibTypeDialog.DIALOG_ERROR,
                            "Save file not found"
                        )
                    }
                }
                .exceptionally {
                    gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, it.toString())
                    null
                }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onSaveGame(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_SAVE)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestCreateFile(filename, MimeType.BINARY_FILE)
                    runBlocking { returnValueFlow.first() }.fileUri
                }
                .thenAccept {
                    if (it != null && it != Uri.EMPTY) {
                        saveGameState(it)
                    } else {
                        gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, "Error access dir")
                    }
                }
                .exceptionally {
                    gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, it.toString())
                    null
                }
        }
    }

    override fun onInputBox(prompt: String?): String {
        gameInterface.showLibDialog(LibTypeDialog.DIALOG_INPUT, prompt ?: "")
        return try {
            runBlocking { returnValueFlow.first() }.dialogTextValue
        } catch (_: Exception) {
            ""
        }
    }

    override fun onGetMsCount(): Int {
        val now = SystemClock.elapsedRealtime()
        if (lastMsCountCallTime == 0L) {
            lastMsCountCallTime = gameStartTime
        }
        val dt = (now - lastMsCountCallTime).toInt()
        lastMsCountCallTime = now
        return dt
    }

    override fun onAddMenuItem(name: String?, imgPath: String?) {
        mutableMenuItemList.add(LibGenItem(name ?: "", imgPath ?: ""))
    }

    override fun onShowMenu(): Int {
        gameInterface.showLibDialog(
            LibTypeDialog.DIALOG_MENU,
            menuItems = mutableMenuItemList
        )
        return try {
            runBlocking { returnValueFlow.first() }.dialogNumValue
        } catch (_: Exception) {
            super.onShowMenu()
        }
    }

    override fun onDeleteMenu() {
        // do nothing
    }

    override fun onSleep(msecs: Int) {
        try {
            Thread.sleep(msecs.toLong())
        } catch (ex: InterruptedException) {
            gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, ex.toString())
        }
    }

    override fun onShowWindow(type: Int, isShow: Boolean) {
        gameInterface.changeVisWindow(LibTypeWindow.entries[type], isShow)
    }
}

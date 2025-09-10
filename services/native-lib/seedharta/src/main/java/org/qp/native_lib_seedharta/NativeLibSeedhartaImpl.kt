package org.qp.native_lib_seedharta

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
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.libndkqsp.jni.NDKLib
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

class NativeLibSeedhartaImpl(
    private val context: Context,
    override var gameInterface: GameInterface,
    override var gameState: LibGameState = LibGameState(),
    override val returnValueFlow: MutableSharedFlow<LibReturnValue> = MutableSharedFlow(),
) : NDKLib(), LibIProxy {

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

        if (!QSPLoadGameWorldFromData(gameData, gameFileFullPath)) {
            showLastQspError()
            return false
        }

        return true
    }

    private fun showLastQspError() {
        val errorData = QSPGetLastErrorData()
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_ERROR,
            inputString =
                """
                Location: ${errorData.locName ?: ""}
                Action: ${errorData.index}
                Line: ${errorData.line}
                Error number: ${errorData.errorNum}
                Description: ${QSPGetErrorDesc(errorData.errorNum) ?: ""}
                """.trimIndent()
        )
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     */
    private fun loadUIConfiguration() {
        val htmlResult = QSPGetVarValues("USEHTML", 0)
        val fSizeResult = QSPGetVarValues("FSIZE", 0)
        val bColorResult = QSPGetVarValues("BCOLOR", 0)
        val fColorResult = QSPGetVarValues("FCOLOR", 0)
        val lColorResult = QSPGetVarValues("LCOLOR", 0)

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
            for (element in QSPGetActionData() ?: return emptyList()) {
                if (element == null) continue
                var tempImagePath = element.image ?: ""
                val tempText = element.text ?: ""

                if (tempImagePath.isEmpty() && tempText.isEmpty()) continue

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
            for (element in QSPGetObjectData() ?: return emptyList()) {
                if (element == null) continue
                var tempImagePath = element.image ?: ""
                val tempText = element.text ?: ""

                if (tempImagePath.isEmpty() && tempText.isEmpty()) continue

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
        libThread = thread(name = "libNDKQSP") {
            QSPInit()
            Looper.prepare()
            libHandler = Handler(Looper.myLooper()!!)
            libThreadInit = true
            while (!libQueue.isEmpty()) {
                libQueue.poll()?.run()
            }
            Looper.loop()
            QSPDeInit()
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
            if (!QSPRestartGame(true)) {
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
        if (!QSPOpenSavedGameFromData(gameData, true)) {
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
            dataToWrite = QSPSaveGameAsData(false) ?: return
        )
    }

    override fun onActionClicked(index: Int) {
        runOnQspThread {
            if (!QSPSetSelActionIndex(index, false)) {
                showLastQspError()
            }
            if (!QSPExecuteSelActionCode(true)) {
                showLastQspError()
            }
        }
    }

    override fun onObjectSelected(index: Int) {
        runOnQspThread {
            if (!QSPSetSelObjectIndex(index, true)) {
                showLastQspError()
            }
        }
    }

    override fun onInputAreaClicked(code: String) {
        runOnQspThread {
            QSPSetInputStrText(code)
            if (!QSPExecUserInput(true)) {
                showLastQspError()
            }
        }
    }

    override fun onUseExecutorString(code: String) {
        runOnQspThread {
            if (!QSPExecString(code, true)) {
                showLastQspError()
            }
        }
    }

    override fun execute(code: String?) {
        runOnQspThread {
            if (!QSPExecString(code, true)) {
                showLastQspError()
            }
        }
    }

    override fun executeCounter() {
        if (libLock.isLocked) return
        runOnQspThread {
            if (!QSPExecCounter(true)) {
                showLastQspError()
            }
        }
    }

    // endregion LibQpProxy
    // region LibQpCallbacks
    override fun RefreshInt() {
        loadUIConfiguration()

        gameState = gameState.copy(
            mainDesc = QSPGetMainDesc() ?: "",
            varsDesc = QSPGetVarsDesc() ?: "",
            actionsList = actionsList,
            objectsList = objectsList
        )

        libStateFlowJob = libScope.launch {
            gameInterface.gameStateFlow.emit(gameState)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun ShowPicture(path: String?) {
        if (!path.isNullOrBlank()) {
            gameInterface.showLibDialog(LibTypeDialog.DIALOG_PICTURE, path)
        }
    }

    override fun SetTimer(msecs: Int) {
        gameInterface.setCountInter(msecs.toLong())
    }

    override fun ShowMessage(message: String?) {
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_MESSAGE,
            inputString = message ?: ""
        )
    }

    @OptIn(ExperimentalContracts::class)
    override fun PlayFile(path: String?, volume: Int) {
        if (!path.isNullOrBlank()) {
            gameInterface.playFile(path, volume)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun IsPlayingFile(path: String?): Boolean {
        if (path.isNullOrBlank()) {
            return false
        } else {
            gameInterface.isPlayingFile(path)
            return runCatching { runBlocking { returnValueFlow.first() }.playFileState }.getOrElse { false }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun CloseFile(path: String?) {
        if (path.isNullOrBlank()) {
            gameInterface.closeAllFiles()
        } else {
            gameInterface.closeFile(path)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun OpenGame(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_LOAD)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestReceiveFile(filename)
                    runBlocking { returnValueFlow.first() }.fileUri
                }
                .thenAccept {
                    if (it != Uri.EMPTY) {
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
    override fun SaveGame(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_SAVE)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestCreateFile(filename, MimeType.BINARY_FILE)
                    runBlocking { returnValueFlow.first() }.fileUri
                }
                .thenAccept {
                    if (it != Uri.EMPTY) {
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

    override fun InputBox(prompt: String?): String {
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_INPUT,
            inputString = prompt ?: ""
        )
        return runCatching { runBlocking { returnValueFlow.first() }.dialogTextValue }.getOrElse { "" }
    }

    override fun GetMSCount(): Int {
        val now = SystemClock.elapsedRealtime()
        if (lastMsCountCallTime == 0L) {
            lastMsCountCallTime = gameStartTime
        }
        val dt = (now - lastMsCountCallTime).toInt()
        lastMsCountCallTime = now
        return dt
    }

    override fun AddMenuItem(name: String?, imgPath: String?) {
        mutableMenuItemList.add(LibGenItem(name ?: "", imgPath ?: ""))
    }

    override fun ShowMenu() {
        gameInterface.showLibDialog(
            LibTypeDialog.DIALOG_MENU,
            menuItems = mutableMenuItemList
        )
        val dialogValue =
            runCatching { runBlocking { returnValueFlow.first() }.dialogNumValue }.getOrElse { -1 }
        if (dialogValue != -1) {
            QSPSelectMenuItem(dialogValue)
        }
    }

    override fun DeleteMenu() {
        // do nothing
    }

    override fun Wait(msecs: Int) {
        runCatching {
            Thread.sleep(msecs.toLong())
        }.onFailure { ex ->
            gameInterface.showLibDialog(
                LibTypeDialog.DIALOG_ERROR,
                ex.toString()
            )
        }
    }

    override fun ShowWindow(type: Int, isShow: Boolean) {
        gameInterface.changeVisWindow(LibTypeWindow.entries[type], isShow)
    }

    @OptIn(ExperimentalContracts::class)
    override fun GetFileContents(path: String?): ByteArray? {
        if (path.isNullOrBlank()) return byteArrayOf()
        gameInterface.requestReceiveFile(path)
        val targetFileUri =
            runCatching { libScope.future { returnValueFlow.first() }.get().fileUri }.getOrElse { Uri.EMPTY }
        if (targetFileUri == Uri.EMPTY) return byteArrayOf()
        return targetFileUri.readFileContents(context)
    }

    @OptIn(ExperimentalContracts::class)
    override fun ChangeQuestPath(path: String?) {
        if (!path.isNullOrBlank()) {
            gameInterface.changeGameDir(path)
        }
    }
}

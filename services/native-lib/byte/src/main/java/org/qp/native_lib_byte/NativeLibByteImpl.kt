package org.qp.native_lib_byte

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat.fromUri
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.child
import com.libqsp.jni.QSPLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.qp.dto.GameInterface
import org.qp.dto.LibGameState
import org.qp.dto.LibGenItem
import org.qp.dto.LibIProxy
import org.qp.dto.LibReturnValue
import org.qp.dto.LibTypeDialog
import org.qp.dto.LibTypePopup
import org.qp.dto.LibTypeWindow
import org.qp.dto.LibUIConfig
import org.qp.utils.FileUtil.getFileContents
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
import java.util.concurrent.CompletionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts

class NativeLibByteImpl(
    private val context: Context,
    override var gameInterface: GameInterface,
    override var gameState: LibGameState = LibGameState(),
    override val returnValueFlow: MutableSharedFlow<LibReturnValue> = MutableSharedFlow(),
) : QSPLib(), LibIProxy {

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

    private fun runOnQspThread(runnable: Runnable) {
        if (libThreadInit) {
            libHandler.post {
                libLock.withLock { runnable.run() }
            }
        } else {
            Log.i(javaClass.simpleName, "runOnQspThread: MISS!")
            libQueue.offer(runnable)
        }
    }

    private fun loadGameWorld(): Boolean {
        val gameData = getFileContents(context, gameState.gameFileUri) ?: return false

        if (!loadGameWorldFromData(gameData, true)) {
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
                Action: ${errorData.actIndex}
                Line: ${errorData.intLineNum}
                Error number: ${errorData.errorNum}
                Description: ${getErrorDesc(errorData.errorNum) ?: ""}
                """.trimIndent()
        )
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     */
    private fun loadUIConfiguration() {
        val htmlResult = getNumVarValue("USEHTML", 0)
        val fSizeResult = getNumVarValue("FSIZE", 0)
        val bColorResult = getNumVarValue("BCOLOR", 0)
        val fColorResult = getNumVarValue("FCOLOR", 0)
        val lColorResult = getNumVarValue("LCOLOR", 0)

        libUIFlowJob = libScope.launch {
            gameInterface.gameUIConfFlow.emit(
                LibUIConfig(
                    useHtml = htmlResult != 0L,
                    fontSize = fSizeResult.toInt(),
                    backColor = bColorResult.toInt(),
                    fontColor = fColorResult.toInt(),
                    linkColor = lColorResult.toInt()
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
                val tempText = element.name ?: ""

                if (tempImagePath.isNotBlank()) {
                    val tempPath = tempImagePath.getFilename().normalizeContentPath()
                    val fileFromPath = gameDir.child(context, tempPath)
                    if (isWritableFile(context, fileFromPath)) {
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
                val tempText = element.name ?: ""

                if (tempText.contains("<img")) {
                    if (!tempText.isContainsHtmlTags()) {
                        val fileFromPath = gameDir.child(context, tempText)
                        if (isWritableFile(context, fileFromPath)) {
                            tempImagePath = fileFromPath.uri.toString()
                        }
                    } else {
                        val tempPath = tempText.getSrcDir()
                        val fileFromPath = gameDir.child(context, tempPath)
                        if (isWritableFile(context, fileFromPath)) {
                            tempImagePath = fileFromPath.uri.toString()
                        }
                    }
                }

                objects.add(LibGenItem(tempText, tempImagePath))
            }

            return objects
        }

    override fun startLibThread() {
        libThread = thread(name = "libQSP") {
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

        val gameData = getFileContents(context, uri) ?: return
        if (!openSavedGameFromData(gameData, true)) {
            showLastQspError()
        }
    }

    override fun saveGameState(uri: Uri) {
        if (!isSameThread(libHandler.looper.thread)) {
            runOnQspThread { saveGameState(uri) }
            return
        }

        val gameData = saveGameAsData(false) ?: return
        writeFileContents(context, uri, gameData)
    }

    override fun onActionClicked(index: Int) {
        runOnQspThread {
            if (!setSelActIndex(index, false)) {
                showLastQspError()
            }
            if (!execSelAction(true)) {
                showLastQspError()
            }
        }
    }

    override fun onObjectSelected(index: Int) {
        runOnQspThread {
            if (!setSelObjIndex(index, true)) {
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

    override fun onRefreshInt(isForced: Boolean, isNewDesc: Boolean) {
        if (isNewDesc) {
            loadUIConfiguration()

            gameState = gameState.copy(
                mainDesc = mainDesc,
                varsDesc = varsDesc,
                actionsList = actionsList,
                objectsList = objectsList
            )

            libStateFlowJob = libScope.launch {
                gameInterface.gameStateFlow.emit(gameState)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onShowImage(file: String?) {
        if (!file.isNullOrBlank()) {
            gameInterface.showLibDialog(LibTypeDialog.DIALOG_PICTURE, file)
        }
    }

    override fun onSetTimer(msecs: Int) {
        gameInterface.setCountInter(msecs.toLong())
    }

    override fun onShowMessage(text: String) {
        gameInterface.showLibDialog(LibTypeDialog.DIALOG_MESSAGE, text)
    }

    @OptIn(ExperimentalContracts::class)
    override fun onPlayFile(file: String?, volume: Int) {
        if (!file.isNullOrBlank()) {
            gameInterface.playFile(file, volume)
        }
    }

    override fun onIsPlayingFile(file: String?): Boolean {
        if (file.isNullOrEmpty()) {
            return false
        } else {
            gameInterface.isPlayingFile(file)
            return try {
                runBlocking { returnValueFlow.first() }.playFileState
            } catch (_: Exception) {
                false
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onCloseFile(file: String?) {
        if (file.isNullOrBlank()) {
            gameInterface.closeAllFiles()
        } else {
            gameInterface.closeFile(file)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onOpenGameStatus(file: String?) {
        if (file.isNullOrEmpty()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_LOAD)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestReceiveFile(file)
                    runBlocking { returnValueFlow.first() }.fileUri
                }
                .thenAccept {
                    if (it != null && it != Uri.EMPTY) {
                        gameInterface.doWithCounterDisabled { loadGameState(it) }
                    } else {
                        throw CompletionException(Throwable("Save file not found"))
                    }
                }
                .exceptionally {
                    gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, it.toString())
                    null
                }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun onSaveGameStatus(file: String?) {
        if (file.isNullOrEmpty()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_SAVE)
        } else {
            CompletableFuture
                .supplyAsync {
                    gameInterface.requestCreateFile(file, MimeType.BINARY_FILE)
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

    override fun onInputBox(text: String): String {
        gameInterface.showLibDialog(LibTypeDialog.DIALOG_INPUT, text)
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
        val dt = now - lastMsCountCallTime
        lastMsCountCallTime = now
        return dt.toInt()
    }

    override fun onShowMenu(items: Array<ListItem>): Int {
        gameInterface.showLibDialog(
            LibTypeDialog.DIALOG_MENU,
            menuItems = items.map { LibGenItem(it.name ?: "", it.image ?: "") }
        )
        return try {
            runBlocking { returnValueFlow.first() }.dialogNumValue
        } catch (_: Exception) {
            super.onShowMenu(items)
        }
    }

    override fun onSleep(msecs: Int) {
        try {
            Thread.sleep(msecs.toLong())
        } catch (ex: InterruptedException) {
            gameInterface.showLibDialog(LibTypeDialog.DIALOG_ERROR, ex.toString())
        }
    }

    override fun onShowWindow(type: Int, toShow: Boolean) {
        gameInterface.changeVisWindow(LibTypeWindow.entries[type], toShow)
    }

    @OptIn(ExperimentalContracts::class)
    override fun onOpenGame(file: String?, isNewGame: Boolean) {
        if (!file.isNullOrBlank()) {
            gameInterface.changeGameDir(file)
        }
    }
}

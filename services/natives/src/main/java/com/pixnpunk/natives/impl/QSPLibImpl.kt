@file:OptIn(ExperimentalContracts::class)

package com.pixnpunk.natives.impl

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileCompat.fromUri
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.child
import com.libqsp.jni.QSPLib
import org.qp.dto.GameInterface
import org.qp.dto.LibGameState
import org.qp.dto.LibGenItem
import org.qp.dto.LibIProxy
import org.qp.dto.LibReturnValue
import org.qp.dto.LibTypeDialog
import org.qp.dto.LibTypePopup
import org.qp.dto.LibTypeWindow
import org.qp.dto.LibUIConfig
import org.qp.utils.FileUtil.isWritableDir
import org.qp.utils.FileUtil.isWritableFile
import org.qp.utils.FileUtil.readFileContents
import org.qp.utils.FileUtil.writeFileContents
import org.qp.utils.HtmlUtil.getSrcDir
import org.qp.utils.HtmlUtil.isContainsHtmlTags
import org.qp.utils.PathUtil.getFilename
import org.qp.utils.PathUtil.normalizeContentPath
import org.qp.utils.ThreadUtil
import org.qp.utils.ThreadUtil.isSameThread
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts

class QSPLibImpl(
    private val context: Context,
    override var gameInterface: GameInterface,
    override var gameState: LibGameState = LibGameState(),
    override val returnValueQueue: ArrayBlockingQueue<LibReturnValue> = ArrayBlockingQueue(1)
) : QSPLib(), LibIProxy {

    private val libLock = ReentrantLock()
    private var libThread: Thread? = null
    private val libQueue: ArrayBlockingQueue<Runnable> = ArrayBlockingQueue(1)
    @Volatile private lateinit var libHandler: Handler
    @Volatile private var libThreadInit = false
    @Volatile private var gameStartTime: Long = 0L
    @Volatile private var lastMsCountCallTime: Long = 0L
    private val currGameDir: DocumentFile?
        get() = fromUri(context, gameState.gameDirUri)

    private inline fun executeQspCommand(block: () -> Boolean): Boolean {
        return if (block()) { true } else { showLastQspError(); false }
    }

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
        val gameData = gameState.gameFileUri.readFileContents(context) ?: return false
        return executeQspCommand { loadGameWorldFromData(gameData, true) }
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

        gameInterface.setUIConfig(
            LibUIConfig(
                useHtml = htmlResult != 0L,
                fontSize = fSizeResult.toInt(),
                backColor = bColorResult.toInt(),
                fontColor = fColorResult.toInt(),
                linkColor = lColorResult.toInt()
            )
        )
    }

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
                    if (fileFromPath.isWritableFile(context)) {
                        tempImagePath = fileFromPath.uri.toString()
                    }
                }

                actions.add(LibGenItem(tempText, tempImagePath))
            }

            return actions
        }

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
            executeQspCommand { restartGame(true) }
        }
    }

    override fun loadGameState(uri: Uri) {
        if (!isSameThread(libHandler.looper.thread)) {
            runOnQspThread { loadGameState(uri) }
            return
        }

        val gameData = uri.readFileContents(context) ?: return
        executeQspCommand { openSavedGameFromData(gameData, true) }
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
            executeQspCommand { setSelActIndex(index, false) }
            executeQspCommand { execSelAction(true) }
        }
    }

    override fun onObjectSelected(index: Int) {
        runOnQspThread { executeQspCommand { setSelObjIndex(index, true) } }
    }

    override fun onInputAreaClicked(code: String) {
        runOnQspThread {
            setInputStrText(code)
            executeQspCommand { execUserInput(true) }
        }
    }

    override fun onUseExecutorString(code: String) {
        runOnQspThread { executeQspCommand { execString(code, true) } }
    }

    override fun execute(code: String) {
        runOnQspThread { executeQspCommand { execString(code, true) } }
    }

    override fun executeCounter() {
        if (libLock.isLocked) return
        runOnQspThread { executeQspCommand { execCounter(true) } }
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

            gameInterface.setGameState(gameState)
        }
    }

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
                returnValueQueue.poll(30, TimeUnit.SECONDS)?.playFileState ?: false
            } catch (_: InterruptedException) {
                false
            }
        }
    }

    override fun onCloseFile(file: String?) {
        if (file.isNullOrBlank()) {
            gameInterface.closeAllFiles()
        } else {
            gameInterface.closeFile(file)
        }
    }

    override fun onOpenGameStatus(file: String?) {
        if (file.isNullOrEmpty()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_LOAD)
        } else {
            gameInterface.requestReceiveFile(file)
            try {
                val fileUri = returnValueQueue.poll(30, TimeUnit.SECONDS)?.fileUri ?: Uri.EMPTY
                if (fileUri != Uri.EMPTY) {
                    gameInterface.doWithCounterDisabled { loadGameState(fileUri) }
                } else {
                    gameInterface.showLibDialog(
                        LibTypeDialog.DIALOG_ERROR,
                        "Save file not found"
                    )
                }
            } catch (_: InterruptedException) {
            }
        }
    }

    override fun onSaveGameStatus(file: String?) {
        if (file.isNullOrEmpty()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_SAVE)
        } else {
            gameInterface.requestCreateFile(file, MimeType.BINARY_FILE)
            try {
                val fileUri = returnValueQueue.poll(30, TimeUnit.SECONDS)?.fileUri ?: Uri.EMPTY
                if (fileUri != Uri.EMPTY) {
                    saveGameState(fileUri)
                } else {
                    gameInterface.showLibDialog(
                        LibTypeDialog.DIALOG_ERROR,
                        "Error access dir"
                    )
                }
            } catch (_: InterruptedException) {
            }
        }
    }

    override fun onInputBox(text: String): String {
        gameInterface.showLibDialog(LibTypeDialog.DIALOG_INPUT, text)

        return try {
            returnValueQueue.poll(30, TimeUnit.SECONDS)?.dialogTextValue ?: ""
        } catch (_: InterruptedException) {
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
            returnValueQueue.poll(30, TimeUnit.SECONDS)?.dialogNumValue ?: -1
        } catch (_: InterruptedException) {
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

    override fun onOpenGame(file: String?, isNewGame: Boolean) {
        if (!file.isNullOrBlank()) {
            gameInterface.changeGameDir(file)
        }
    }
}
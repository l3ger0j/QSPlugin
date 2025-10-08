@file:OptIn(ExperimentalContracts::class)

package com.pixnpunk.natives.impl

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
import com.libsdhqs.jni.QSLibSDH
import kotlinx.coroutines.Runnable
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
import org.qp.utils.ThreadUtil.isSameThread
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts

class QSLibSDHImpl(
    private val context: Context,
    override var gameInterface: GameInterface,
    override var gameState: LibGameState = LibGameState(),
    override val returnValueQueue: ArrayBlockingQueue<LibReturnValue> = ArrayBlockingQueue(1)
) : QSLibSDH(), LibIProxy {

    private val libLock = ReentrantLock()
    private var libThread: Thread? = null
    private val libQueue: ArrayBlockingQueue<Runnable> = ArrayBlockingQueue(1)
    @Volatile private lateinit var libHandler: Handler
    @Volatile private var libThreadInit = false
    @Volatile private var gameStartTime: Long = 0L
    @Volatile private var lastMsCountCallTime: Long = 0L
    private val currGameDir: DocumentFile?
        get() = fromUri(context, gameState.gameDirUri)

    private val mutableMenuItemList: MutableList<LibGenItem> = mutableListOf()

    private inline fun executeQspCommand(block: () -> Boolean): Boolean {
        return if (block()) { true } else { showLastQspError(); false }
    }

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

//        val fd = context.contentResolver
//            .openFileDescriptor(gameFileUri, "r")
//            .runCatching { this?.fileDescriptor ?: FileDescriptor() }
//            .getOrDefault(FileDescriptor())

        return executeQspCommand { loadGameWorldFromData(gameData, gameFileFullPath) }
    }

    private fun showLastQspError() {
        val errorData = getLastErrorData()
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_ERROR,
            inputString =
                """
                Location: ${errorData?.locName ?: ""}
                Action: ${errorData?.index}
                Line: ${errorData?.line}
                Error number: ${errorData?.errorNum}
                Description: ${
                    if (errorData != null) { getErrorDesc(errorData.errorNum) ?: "" } else { "" }
                }
                """.trimIndent()
        )
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     */
    private fun loadUIConfiguration() {
        val htmlResult = getVarValues("USEHTML", 0)
        val fSizeResult = getVarValues("FSIZE", 0)
        val bColorResult = getVarValues("BCOLOR", 0)
        val fColorResult = getVarValues("FCOLOR", 0)
        val lColorResult = getVarValues("LCOLOR", 0)

        gameInterface.setUIConfig(
            LibUIConfig(
                useHtml = htmlResult?.intValue != 0,
                fontSize = fSizeResult?.intValue ?: 0,
                backColor = bColorResult?.intValue ?: 0,
                fontColor = fColorResult?.intValue ?: 0,
                linkColor = lColorResult?.intValue ?: 0
            )
        )
    }

    private val actionsList: List<LibGenItem>
        get() {
            val gameDir = currGameDir
            if (!gameDir.isWritableDir(context)) return emptyList()

            val actions = mutableListOf<LibGenItem>()
            for (element in getActions() ?: return emptyList()) {
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

    private val objectsList: List<LibGenItem>
        get() {
            val gameDir = currGameDir
            if (!gameDir.isWritableDir(context)) return emptyList()

            val objects = mutableListOf<LibGenItem>()
            for (element in getObjects() ?: return emptyList()) {
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

//        val fd = context.contentResolver
//            .openFileDescriptor(uri, "r")
//            .runCatching { this?.fileDescriptor ?: FileDescriptor() }
//            .getOrDefault(FileDescriptor())

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
            executeQspCommand { setSelActionIndex(index, false) }
            executeQspCommand { executeSelActionCode(true) }
        }
    }

    override fun onObjectSelected(index: Int) {
        runOnQspThread { executeQspCommand { setSelObjectIndex(index, true) } }
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

    // endregion LibQpProxy
    // region LibQpCallbacks
    override fun onRefreshInt() {
        loadUIConfiguration()

        gameState = gameState.copy(
            mainDesc = getMainDesc() ?: "",
            varsDesc = getVarsDesc() ?: "",
            actionsList = actionsList,
            objectsList = objectsList
        )

        gameInterface.setGameState(gameState)
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
                returnValueQueue.poll(30, TimeUnit.SECONDS)?.playFileState ?: false
            } catch (_: InterruptedException) {
                false
            }
        }
    }

    override fun onCloseFile(path: String?) {
        if (path.isNullOrBlank()) {
            gameInterface.closeAllFiles()
        } else {
            gameInterface.closeFile(path)
        }
    }

    override fun onOpenGame(path: String?) {
        if (!path.isNullOrBlank()) {
            gameInterface.changeGameDir(path)
        }
    }

    override fun onOpenGameStatus(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_LOAD)
        } else {
            gameInterface.requestReceiveFile(filename)
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

    override fun onSaveGameStatus(filename: String?) {
        if (filename.isNullOrBlank()) {
            gameInterface.showLibPopup(LibTypePopup.POPUP_SAVE)
        } else {
            gameInterface.requestCreateFile(filename, MimeType.BINARY_FILE)
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

    override fun onInputBox(prompt: String?): String {
        gameInterface.showLibDialog(
            dialogType = LibTypeDialog.DIALOG_INPUT,
            inputString = prompt ?: ""
        )

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
        val dt = (now - lastMsCountCallTime).toInt()
        lastMsCountCallTime = now
        return dt
    }

    override fun onAddMenuItem(name: String?, imgPath: String?) {
        mutableMenuItemList.add(LibGenItem(name ?: "", imgPath ?: ""))
    }

    override fun onShowMenuOld() {
        gameInterface.showLibDialog(
            LibTypeDialog.DIALOG_MENU,
            menuItems = mutableMenuItemList
        )

        selectMenuItem(
            index =
                try {
                    returnValueQueue.poll(30, TimeUnit.SECONDS)?.dialogNumValue ?: -1
                } catch (_: InterruptedException) {
                    -1
                }
        )
    }

    override fun onSleep(msecs: Int) {
        runCatching {
            Thread.sleep(msecs.toLong())
        }.onFailure { ex ->
            gameInterface.showLibDialog(
                LibTypeDialog.DIALOG_ERROR,
                ex.toString()
            )
        }
    }

    override fun onShowWindow(type: Int, isShow: Boolean) {
        gameInterface.changeVisWindow(LibTypeWindow.entries[type], isShow)
    }

    override fun onGetFileContents(path: String?): ByteArray? {
        if (path.isNullOrBlank()) return byteArrayOf()
        gameInterface.requestReceiveFile(path)

        val targetFileUri = try {
            returnValueQueue.poll(30, TimeUnit.SECONDS)?.fileUri ?: Uri.EMPTY
        } catch (_: InterruptedException) {
            Uri.EMPTY
        }
        if (targetFileUri == Uri.EMPTY) return byteArrayOf()
        return targetFileUri.readFileContents(context)
    }
}
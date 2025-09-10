package org.qp.supervisor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat.doesExist
import com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.makeFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.qp.audio.AudioPlayerService
import org.qp.dto.GameInterface
import org.qp.dto.LibGameState
import org.qp.dto.LibGenItem
import org.qp.dto.LibReturnValue
import org.qp.dto.LibTypeDialog
import org.qp.dto.LibTypePopup
import org.qp.dto.LibTypeWindow
import org.qp.dto.LibUIConfig
import org.qp.native_lib_byte.NativeLibByteImpl
import org.qp.native_lib_seedharta.NativeLibSeedhartaImpl
import org.qp.native_lib_sonnix.NativeLibSonnixImpl
import org.qp.settings.SettingsRepo
import org.qp.utils.FileUtil.isWritableDir
import org.qp.utils.FileUtil.isWritableFile
import org.qp.utils.PathUtil.normalizeContentPath
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.contracts.ExperimentalContracts

class SupervisorService(
    private val audioPlayer: AudioPlayerService,
    private val settingsRepo: SettingsRepo
) : ViewModel(), GameInterface, KoinComponent {

    private val context: Context by inject()

    private var counterNativeByteJob: Job? = null
    private var counterNativeSonnixJob: Job? = null
    private var counterNativeSeedhartaJob: Job? = null
    private var updateSettingsJob: Job? = null
    private val supervisorServiceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val mLibVersion: Int
        get() = settingsRepo.settingsState.value.nativeLibVersion
    private val libNativeByte = NativeLibByteImpl(context, this)
    private val libNativeSonnix = NativeLibSonnixImpl(context, this)
    private val libNativeSeedharta = NativeLibSeedhartaImpl(context, this)

    private var gameDirUri: Uri = Uri.EMPTY

    override val gameStateFlow: MutableStateFlow<LibGameState> = MutableStateFlow(LibGameState())
    override val gameUIConfFlow: MutableStateFlow<LibUIConfig> = MutableStateFlow(LibUIConfig())

    private val _gameDialogFlow: MutableSharedFlow<Triple<LibTypeDialog, String, List<LibGenItem>>> = MutableSharedFlow()
    private val _gamePopupFlow: MutableSharedFlow<LibTypePopup> = MutableSharedFlow()
    private val _gameElementVis: MutableSharedFlow<Pair<LibTypeWindow, Boolean>> = MutableSharedFlow()
    val gameDialogFlow: SharedFlow<Triple<LibTypeDialog, String, List<LibGenItem>>> = _gameDialogFlow
    val gamePopupFlow: SharedFlow<LibTypePopup> = _gamePopupFlow
    val gameElementVis: SharedFlow<Pair<LibTypeWindow, Boolean>> = _gameElementVis
    val returnDialogFlow: MutableSharedFlow<LibReturnValue> = libNativeByte.returnValueFlow

    @Volatile private var counterInterval = 500L
    private val counterNativeByteTask: suspend CoroutineScope.() -> Unit = {
        while (isActive) {
            libNativeByte.executeCounter()
            delay(counterInterval)
        }
    }
    private val counterNativeSonnixTask: suspend CoroutineScope.() -> Unit = {
        while (isActive) {
            libNativeSonnix.executeCounter()
            delay(counterInterval)
        }
    }
    private val counterNativeSeedhartaTask: suspend CoroutineScope.() -> Unit = {
        while (isActive) {
            libNativeSeedharta.executeCounter()
            delay(counterInterval)
        }
    }

    fun startService(
        bundle: Bundle
    ) {
        val id = bundle.getLong("gameId", 0)
        val title = bundle.getString("gameTitle", "Undefined")
        val dirUri = bundle.getParcelable("gameDirUri") ?: Uri.EMPTY
        val fileUri = bundle.getParcelable("gameFileUri") ?: Uri.EMPTY

        gameDirUri = dirUri

        when (mLibVersion) {
            595 -> {
                libNativeByte.startLibThread()
                libNativeByte.runGame(id, title, dirUri, fileUri)
            }
            575 -> {
                libNativeSonnix.startLibThread()
                libNativeSonnix.runGame(id, title, dirUri, fileUri)
            }
            570 -> {
                libNativeSeedharta.startLibThread()
                libNativeSeedharta.runGame(id, title, dirUri, fileUri)
            }
        }

        updateSettingsJob = supervisorServiceScope.launch {
            settingsRepo.settingsState.combine(gameUIConfFlow) { appSet, libSet ->
                appSet.copy(
                    isUseHtml = libSet.useHtml,
                    textColor = if (appSet.isUseGameTextColor && libSet.fontColor != 0) { libSet.fontColor } else { appSet.textColor },
                    backColor = if (appSet.isUseGameBackgroundColor && libSet.backColor != 0) { libSet.backColor } else { appSet.backColor },
                    linkColor = if (appSet.isUseGameLinkColor && libSet.linkColor != 0) { libSet.linkColor } else { appSet.linkColor },
                    fontSize = if (appSet.isUseGameFont && libSet.fontSize != 0) { libSet.fontSize } else { appSet.fontSize }
                )
            }.collect { settingsRepo.emitValue(it) }
        }
    }

    override fun onCleared() {
        libNativeByte.stopLibThread()
        libNativeSonnix.stopLibThread()
        libNativeSeedharta.stopLibThread()
        updateSettingsJob?.cancel()
        counterNativeByteJob?.cancel()
        counterNativeSonnixJob?.cancel()
        counterNativeSeedhartaJob?.cancel()
    }

    fun onSaveFile(fileUri: Uri) {
        when (mLibVersion) {
            595 -> {
                libNativeByte.saveGameState(fileUri)
            }
            575 -> {
                libNativeSonnix.saveGameState(fileUri)
            }
            570 -> {
                libNativeSeedharta.saveGameState(fileUri)
            }
        }
    }

    fun onLoadFile(fileUri: Uri) {
        doWithCounterDisabled {
            when (mLibVersion) {
                595 -> {
                    libNativeByte.loadGameState(fileUri)
                }
                575 -> {
                    libNativeSonnix.loadGameState(fileUri)
                }
                570 -> {
                    libNativeSeedharta.loadGameState(fileUri)
                }
            }
        }
    }

    fun onCodeExec(execCode: String) {
        when (mLibVersion) {
            595 -> {
                libNativeByte.execute(execCode)
            }
            575 -> {
                libNativeSonnix.execute(execCode)
            }
            570 -> {
                libNativeSeedharta.execute(execCode)
            }
        }
    }

    fun onRestartGame() {
        when (mLibVersion) {
            595 -> {
                libNativeByte.restartGame()
            }
            575 -> {
                libNativeSonnix.restartGame()

            }
            570 -> {
                libNativeSeedharta.restartGame()
            }
        }
    }

    fun onActionClicked(index: Int) {
        when (mLibVersion) {
            595 -> {
                libNativeByte.onActionClicked(index)
            }
            575 -> {
                libNativeSonnix.onActionClicked(index)
            }
            570 -> {
                libNativeSeedharta.onActionClicked(index)
            }
        }
    }

    fun onObjectSelected(index: Int) {
        when (mLibVersion) {
            595 -> {
                libNativeByte.onObjectSelected(index)
            }
            575 -> {
                libNativeSonnix.onObjectSelected(index)
            }
            570 -> {
                libNativeSeedharta.onObjectSelected(index)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun requestReceiveFile(filePath: String) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        CompletableFuture
            .supplyAsync { doesExist(context, filePath) }
            .thenApplyAsync {
                if (it) {
                    return@thenApplyAsync fromFullPath(context, filePath)
                } else {
                    return@thenApplyAsync gameDir.child(context, filePath)
                }
            }
            .thenApplyAsync {
                return@thenApplyAsync if (it.isWritableFile(context)) {
                    it.uri
                } else {
                    Uri.EMPTY
                }
            }
            .thenAcceptAsync {
                supervisorServiceScope.launch {
                    libNativeByte.returnValueFlow.emit(LibReturnValue(fileUri = it))
                }
            }
            .exceptionally {
                Log.e(javaClass.simpleName, "requestReceiveFile: ERROR!", it)
                null
            }
    }

    @OptIn(ExperimentalContracts::class)
    override fun requestCreateFile(path: String, mimeType: String) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        CompletableFuture
            .supplyAsync {
                gameDir.makeFile(context, path, mimeType)
            }
            .thenApplyAsync {
                if (it.isWritableFile(context)) {
                    return@thenApplyAsync it.uri
                } else {
                    return@thenApplyAsync Uri.EMPTY
                }
            }
            .thenAcceptAsync {
                if (it == Uri.EMPTY) return@thenAcceptAsync
                supervisorServiceScope.launch {
                    libNativeByte.returnValueFlow.emit(LibReturnValue(fileUri = it))
                }
            }
            .exceptionally {
                Log.e(javaClass.simpleName, "requestReceiveFile: ERROR!", it)
                null
            }
    }

    @OptIn(ExperimentalContracts::class)
    override fun isPlayingFile(filePath: String) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        val normPath = filePath.normalizeContentPath()
        CompletableFuture
            .supplyAsync { gameDir.child(context, normPath) }
            .thenApplyAsync {
                if (it.isWritableFile(context)) {
                    return@thenApplyAsync it.uri
                } else {
                    val errorMsg = "Sound file by path: $filePath not writable"
                    throw CompletionException(RuntimeException(errorMsg))
                }
            }
            .exceptionally {
                Log.e(javaClass.simpleName, "isPlayingFile: ERROR!", it)
                Uri.EMPTY
            }
            .thenApply { fileUri -> audioPlayer.isPlayingFile(fileUri) }
            .thenAcceptAsync {
                if (it != null) {
                    supervisorServiceScope.launch {
                        libNativeByte.returnValueFlow.emit(LibReturnValue(playFileState = true))
                    }
                }
            }
            .exceptionally {
                Log.e(javaClass.simpleName, "isPlayingFile: ERROR!", it)
                null
            }
    }

    override fun closeAllFiles() {
        try {
            audioPlayer.closeAllFiles()
        } catch (e: Exception) {
            // doShowErrorDialog(t.toString(), ErrorType.EXCEPTION)
            Log.e(javaClass.simpleName, "closeAllFiles: ERROR!", e)
        }
    }

    @OptIn(ExperimentalContracts::class)
    override fun closeFile(filePath: String?) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        val normPath = filePath ?: "".normalizeContentPath()
        CompletableFuture
            .supplyAsync { gameDir.child(context, normPath) }
            .thenApply {
                return@thenApply if (it.isWritableFile(context)) {
                    it.uri
                } else {
                    Uri.EMPTY
                }
            }
            .thenApply {
                if (it !== Uri.EMPTY) {
                    audioPlayer.closeFile(it)
                } else {
                    val errorMsg = "Sound file by path: $filePath not writable"
                    throw CompletionException(RuntimeException(errorMsg))
                }
            }
            .exceptionally {
//                if (getSettingsController().isUseMusicDebug) {
//                    doShowErrorDialog(throwable.toString(), ErrorType.SOUND_ERROR)
//                }
                Log.e(javaClass.simpleName, "closeFile: ERROR!", it)
                null
            }
    }

    @OptIn(ExperimentalContracts::class)
    override fun playFile(path: String?, volume: Int) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        val normPath = path ?: "".normalizeContentPath()
        CompletableFuture
            .supplyAsync { gameDir.child(context, normPath) }
            .thenApply {
                return@thenApply if (it.isWritableFile(context)) {
                    it.uri
                } else {
                    Uri.EMPTY
                }
            }
            .thenApply {
                if (it != Uri.EMPTY) {
                    audioPlayer.playFile(context, it, volume)
                } else {
                    val errorMsg = "Sound file by path: $path not writable"
                    throw CompletionException(RuntimeException(errorMsg))
                }
            }
            .exceptionally {
//                if (getSettingsController().isUseMusicDebug) {
//                    doShowErrorDialog(throwable.toString(), ErrorType.SOUND_ERROR)
//                }
                Log.e(javaClass.simpleName, "playFile: ERROR!", it)
                null
            }
    }

    @OptIn(ExperimentalContracts::class)
    override fun changeGameDir(filePath: String) {
        val gameDir = gameDirUri.toDocumentFile(context)
        if (!gameDir.isWritableDir(context)) return

        CompletableFuture
            .supplyAsync { doesExist(context, filePath) }
            .thenAcceptAsync {
                if (it) {
//                    val oldDirPath = gameDir.getAbsolutePath(this)
//                    if (oldDirPath != filePath) {
//                        val newGameDir = fromFullPath(this, filePath, requiresWriteAccess = true)
//                        if (isWritableDir(this, newGameDir)) {
//                            gameDirUri = newGameDir.uri
//                        }
//                    }
                }
            }
    }

    override fun showLibDialog(
        dialogType: LibTypeDialog,
        inputString: String,
        menuItems: List<LibGenItem>
    ) {
        supervisorServiceScope.launch {
            _gameDialogFlow.emit(Triple(dialogType, inputString, menuItems))
        }
    }

    override fun showLibPopup(popupType: LibTypePopup) {
        supervisorServiceScope.launch {
            _gamePopupFlow.emit(popupType)
        }
    }

    override fun changeVisWindow(
        type: LibTypeWindow,
        show: Boolean
    ) {
        supervisorServiceScope.launch {
            _gameElementVis.emit(type to show)
        }
    }

    override fun setCountInter(delayMillis: Long) {
        counterInterval = delayMillis
    }

    override fun doWithCounterDisabled(runnable: Runnable) {
        when (mLibVersion) {
            595 -> {
                counterNativeByteJob?.cancel()
                runnable.run()
                counterNativeByteJob =
                    supervisorServiceScope.launch(block = counterNativeByteTask)
            }
            575 -> {
                counterNativeSonnixJob?.cancel()
                runnable.run()
                counterNativeSonnixJob =
                    supervisorServiceScope.launch(block = counterNativeSonnixTask)
            }
            570 -> {
                counterNativeSeedhartaJob?.cancel()
                runnable.run()
                counterNativeSeedhartaJob =
                    supervisorServiceScope.launch(block = counterNativeSeedhartaTask)
            }
        }
    }
}
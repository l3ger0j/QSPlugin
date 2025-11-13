package com.pixnpunk.qsplugin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process.killProcess
import android.os.Process.myPid
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.provider.DocumentsContractCompat.buildDocumentUriUsingTree
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.anggrayudi.storage.SimpleStorage.Companion.cleanupRedundantUriPermissions
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.canModify
import com.anggrayudi.storage.file.id
import com.arkivanov.decompose.defaultComponentContext
import com.pixnpunk.audio.AudioPlayerViewModel
import com.pixnpunk.dto.GameSettings
import com.pixnpunk.qsplugin.mvi.RootStore
import com.pixnpunk.settings.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var root: RealRootComponent

    private val audioPlayer: AudioPlayerViewModel by inject()
    private val settingsRepo: SettingsRepo by inject()

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val requestFolderAccess: ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            if (it != null) {
                val contentResolver = applicationContext.contentResolver
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                thread { cleanupRedundantUriPermissions(this.applicationContext) }

                gameDirUri = it

                val file = gameFileUri.toDocumentFile(this)
                if (file != null) {
                    val uriFile = buildDocumentUriUsingTree(gameDirUri, file.id)
                    if (uriFile != null) {
                        gameFileUri = uriFile
                    }
                }

                root.runGame(gameId, gameTitle, gameDirUri, gameFileUri)
            }
        }
    private val requestCreateFile: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { root.onSaveFileResult(it) }
            }
        }
    private val requestOpenFile: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { root.onLoadFileResult(it) }
            }
        }

    private var gameId: Long = 1L
    private var gameTitle: String = "Title"
    private var gameDirUri: Uri = Uri.EMPTY
    private var gameFileUri: Uri = Uri.EMPTY

    companion object {
        private const val EXTRA_GAME_ID = "gameId"
        private const val EXTRA_GAME_TITLE = "gameTitle"
        private const val EXTRA_GAME_DIR_URI = "gameDirUri"
        private const val EXTRA_GAME_FILE_URI = "gameFileUri"
        private const val EXTRA_GAME_SETTINGS = "gameAppSettings"
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(audioPlayer)

        if (settingsRepo.settingsState.value.isImmersModeEnabled) {
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                ViewCompat.onApplyWindowInsets(view, windowInsets)
            }
        }

        if (savedInstanceState != null) {
            val dirUri = savedInstanceState.getParcelable<Uri>("dirUri")
            if (dirUri != null) {
                gameDirUri = dirUri
            }

            val fileUri = savedInstanceState.getParcelable<Uri>("fileUri")
            if (fileUri != null) {
                gameFileUri = fileUri
            }
        } else {
            intent?.let {
                gameId = it.getLongExtra(EXTRA_GAME_ID, 1L)
                gameTitle = it.getStringExtra(EXTRA_GAME_TITLE) ?: "Title"
                gameDirUri = it.getParcelableExtra(EXTRA_GAME_DIR_URI) ?: Uri.EMPTY
                gameFileUri = it.getParcelableExtra(EXTRA_GAME_FILE_URI) ?: Uri.EMPTY
                val gameSettingsString = it.getStringExtra(EXTRA_GAME_SETTINGS)
                if (gameSettingsString != null) {
                    settingsRepo.emitValue(Json.decodeFromString<GameSettings>(gameSettingsString))
                }
            }
        }

        root = RealRootComponent(
            componentContext = defaultComponentContext(),
            onConfirmPerm = {
                requestFolderAccess.launch(gameDirUri)
            },
            onStartActivity = {
                startActivity(it)
            }
        )

        val gameDirFile = gameDirUri.toDocumentFile(this)
        if (gameDirFile != null) {
            if (!gameDirFile.canModify(this)) {
                root.doShowReqPermDialog()
            } else {
                if (!root.model.value.isGameRunning) {
                    root.runGame(gameId, gameTitle, gameDirUri, gameFileUri)
                }
            }
        }

        mainScope.launch {
            root.label.collect {
                when (it) {
                    is RootStore.Label.ShowDialogDefault ->
                        root.doShowDefaultDialog(it.inputString, it.dialogState)

                    is RootStore.Label.ShowDialogMenu ->
                        root.doShowDialogMenu(it.inputString, it.inputListItems)

                    is RootStore.Label.ShowDialogMessage -> root.doShowDialogMessage(it.inputString)

                    is RootStore.Label.ShowLoadFilePicker -> requestOpenFile.launch(it.intent)

                    is RootStore.Label.ShowSaveFilePicker -> requestCreateFile.launch(it.intent)
                }
            }
        }

        setContent {
            val settings by settingsRepo.settingsState.collectAsState()

            RootContent(
                isDarkTheme = when {
                    settings.theme.contains("auto") -> isSystemInDarkTheme()
                    settings.theme.contains("dark") -> true
                    settings.theme.contains("light") -> false
                    else -> false
                },
                component = root,
                onFinish = { killProcess(myPid()) }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("dirUri", gameDirUri)
        outState.putParcelable("fileUri", gameFileUri)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}
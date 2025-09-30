package org.qp.qsplugin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.koin.android.ext.android.inject
import org.qp.audio.AudioPlayerViewModel
import org.qp.dialogs.presentation.DialogsMainContent
import org.qp.dto.GameSettings
import org.qp.extra.presentation.ExtraContent
import org.qp.main.presentation.MainContent
import org.qp.presentation.InputContent
import org.qp.presentation.ObjectContent
import org.qp.qsplugin.mvi.RootStore
import org.qp.qsplugin.ui.theme.QSPluginTheme
import org.qp.settings.SettingsRepo
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var root: RealRootComponent

    private val audioPlayer: AudioPlayerViewModel by inject()
    private val settingsRepo: SettingsRepo by inject()

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
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) return@registerForActivityResult
            val resultData: Intent? = it.data
            if (resultData == null) return@registerForActivityResult
            val uri = resultData.data

            if (uri != null) {
                root.onSaveFile(uri)
            }
        }
    private val requestOpenFile: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) return@registerForActivityResult
            val resultData: Intent? = it.data
            if (resultData == null) return@registerForActivityResult
            val uri = resultData.data

            if (uri != null) {
                root.onLoadFile(uri)
            }
        }

    private var isPauseBlock: Boolean = true
    private var gameId: Long = 1L
    private var gameTitle: String = "Title"
    private var gameDirUri: Uri = Uri.EMPTY
    private var gameFileUri: Uri = Uri.EMPTY

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (settingsRepo.settingsState.value.isUseImmersiveMode) {
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
            isPauseBlock = false

            val dirUri = savedInstanceState.getParcelable<Uri>("dirUri")
            if (dirUri != null) {
                gameDirUri = dirUri
            }

            val fileUri = savedInstanceState.getParcelable<Uri>("fileUri")
            if (fileUri != null) {
                gameFileUri = fileUri
            }
        } else {
            val id = intent?.getLongExtra("gameId", 1L)
            if (id != null) {
                gameId = id
            }

            val title = intent?.getStringExtra("gameTitle")
            if (title != null) {
                gameTitle = title
            }


            val dirUri = intent?.getParcelableExtra<Uri?>("gameDirUri")
            if (dirUri != null) {
                gameDirUri = dirUri
            }

            val fileUri = intent?.getParcelableExtra<Uri?>("gameFileUri")
            if (fileUri != null) {
                gameFileUri = fileUri
            }

            val settings = intent?.getParcelableExtra<GameSettings?>("gameAppSettings")
            if (settings != null) {
                settingsRepo.emitValue(settings)
            }
        }

        root = RealRootComponent(
            componentContext = defaultComponentContext(),
            appContext = this.applicationContext,
        )

        val gameDirFile = gameDirUri.toDocumentFile(this)
        if (gameDirFile != null) {
            if (!gameDirFile.canModify(this)) {
                requestFolderAccess.launch(gameDirUri)
            } else {
                if (!root.model.value.isGameRunning) {
                    root.runGame(gameId, gameTitle, gameDirUri, gameFileUri)
                }
            }
        }

        setContent {
            val state = root.model.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)

            val stack by root.childStack.subscribeAsState()
            val dialogSlot by root.dialogSlot.subscribeAsState()
            val activeComponent = stack.active.instance

            LaunchedEffect(Unit) {
                root.label.collect {
                    when (it) {
                        is RootStore.Label.ShowDialogDefault -> root.doShowDefaultDialog(
                            it.inputString,
                            it.dialogState
                        )

                        is RootStore.Label.ShowDialogMenu -> root.doShowDialogMenu(
                            it.inputString,
                            it.inputListItems
                        )

                        is RootStore.Label.ShowDialogMessage -> root.doShowDialogMessage(
                            it.inputString
                        )

                        is RootStore.Label.ShowLoadFileActivity -> {
                            requestOpenFile.launch(it.intent)
                        }

                        is RootStore.Label.ShowSaveFileActivity -> {
                            requestCreateFile.launch(it.intent)
                        }
                    }
                }
            }

            QSPluginTheme {
                dialogSlot.child?.instance?.also {
                    DialogsMainContent(it)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        RootAppBar(
                            component = root,
                            activeComponent = activeComponent,
                            onFinishActivity = {
                                onBackPressedDispatcher.onBackPressed()
                                finish()
                            }
                        )
                    },
                    bottomBar = { RootNavBar(root, activeComponent) }
                ) { paddingValues ->
                    Children(
                        modifier = Modifier.padding(paddingValues),
                        stack = root.childStack
                    ) { child ->
                        when (val instance = child.instance) {
                            is RootComponent.Child.MainChild -> {
                                MainContent(instance.component)
                            }

                            is RootComponent.Child.ExtraChild -> {
                                ExtraContent(instance.component)
                            }

                            is RootComponent.Child.ObjectChild -> {
                                ObjectContent(instance.component)
                            }

                            is RootComponent.Child.InputChild -> {
                                InputContent(instance.component)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("dirUri", gameDirUri)
        outState.putParcelable("fileUri", gameFileUri)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        if (!isPauseBlock) {
            audioPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (root.model.value.isGameRunning) {
            audioPlayer.resume()
        }
    }
}
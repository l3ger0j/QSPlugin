package com.pixnpunk.qsplugin

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.MimeType.getMimeTypeFromFileName
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.makeFolder
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.child
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.pixnpunk.dialogs.presentation.DialogConfig
import com.pixnpunk.dialogs.presentation.DialogState
import com.pixnpunk.dialogs.presentation.DialogsComponent
import com.pixnpunk.dialogs.presentation.RealDialogsComponent
import com.pixnpunk.dto.LibGenItem
import com.pixnpunk.extra.presentation.RealExtraComponent
import com.pixnpunk.main.presentation.RealMainComponent
import com.pixnpunk.natives.SupervisorViewModel
import com.pixnpunk.`object`.presentation.RealObjectComponent
import com.pixnpunk.qsplugin.mvi.RealRootStore
import com.pixnpunk.qsplugin.mvi.RootStore
import com.pixnpunk.utils.Base64Util.decodeBase64
import com.pixnpunk.utils.Base64Util.isBase64
import com.pixnpunk.utils.FileUtil.isWritableDir
import com.pixnpunk.utils.FileUtil.isWritableFile
import com.pixnpunk.utils.HtmlUtil.isContainsHtmlTags
import com.pixnpunk.utils.HtmlUtil.removeHtmlTags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.FileNotFoundException
import kotlin.contracts.ExperimentalContracts

class RealRootComponent(
    private val componentContext: ComponentContext,
    private val onConfirmPerm: () -> Unit,
    private val onStartActivity: (Intent) -> Unit
) : ComponentContext by componentContext, RootComponent, KoinComponent {

    private val supervisorService: SupervisorViewModel by inject()

    private var rootDir: DocumentFile? = null

    private val navigation = StackNavigation<ChildConfig>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()
    private val store = instanceKeeper.getStore {
        RealRootStore(DefaultStoreFactory()).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val model: StateFlow<RootStore.State> = store.stateFlow
    override val label = store.labels
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = serializer(),
        initialConfiguration = ChildConfig.Main,
        childFactory = ::createChild
    )
    override val dialogSlot: Value<ChildSlot<*, DialogsComponent>> =
        childSlot(
            source = dialogNavigation,
            serializer = DialogConfig.serializer()
        ) { config, context ->
            RealDialogsComponent(
                componentContext = context,
                dialogConfig = config,
                onCleanError = ::onCleanError,
                onConfirmPerm = onConfirmPerm,
                onEnterValue = { onEnterValue(it.first, it.second) },
                onSelMenuItem = { onSelMenuItem(it) },
                onDismissed = dialogNavigation::dismiss
            )
        }

    fun onSaveFileResult(fileUri: Uri) {
        store.accept(RootStore.Intent.OnSaveFile(fileUri))
    }

    fun onLoadFileResult(fileUri: Uri) {
        store.accept(RootStore.Intent.OnLoadFile(fileUri))
    }

    override fun doShowReqPermDialog() {
        dialogNavigation.activate(
            configuration = DialogConfig(
                dialogState = DialogState.DIALOG_PERMISSION,
            )
        )
    }

    override fun doCreateSaveIntent() {
        store.accept(RootStore.Intent.CreateSaveIntent)
    }

    override fun doCreateLoadIntent() {
        store.accept(RootStore.Intent.CreateLoadIntent)
    }

    private fun onSelMenuItem(index: Int) {
        store.accept(RootStore.Intent.OnSelectMenuItem(index))
    }

    private fun onEnterValue(inputString: String, isBox: Boolean) {
        store.accept(RootStore.Intent.OnEnterValue(inputString, isBox))
    }

    private fun onCleanError() {
        val oldConfig = dialogSlot.child?.configuration as DialogConfig
        dialogNavigation.activate(
            configuration = oldConfig.copy(
                isDialogInputError = false,
                dialogInputErrorString = ""
            )
        )
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun navigation(child: ChildConfig) {
        navigation.bringToFront(child)
    }

    override fun doShowDefaultDialog(
        inputString: String,
        dialogState: DialogState
    ) {
        if (dialogState == DialogState.DIALOG_INPUT) {
            dialogNavigation.activate(
                configuration = DialogConfig(
                    dialogState = dialogState,
                    dialogInputString = inputString,
                    isDialogInputBox = true
                )
            )
        } else {
            dialogNavigation.activate(
                configuration = DialogConfig(
                    dialogState = dialogState,
                    dialogInputString = inputString
                )
            )
        }
    }

    override fun doShowDialogMessage(
        inputString: String
    ) {
        val oldConfig = dialogSlot.child?.configuration as? DialogConfig
        if (oldConfig != null && oldConfig.dialogState == DialogState.DIALOG_INPUT) {
            dialogNavigation.activate(
                configuration = oldConfig.copy(
                    isDialogInputError = true,
                    dialogInputErrorString = inputString
                )
            )
        } else {
            dialogNavigation.activate(
                configuration = DialogConfig(
                    dialogState = DialogState.DIALOG_MESSAGE,
                    dialogInputString = inputString
                )
            )
        }
    }

    override fun doShowDialogMenu(
        inputString: String,
        inputListItems: List<LibGenItem>
    ) {
        dialogNavigation.activate(
            configuration = DialogConfig(
                dialogState = DialogState.DIALOG_MENU,
                dialogInputString = inputString,
                dialogMenuItems = inputListItems
            )
        )
    }

    override fun doShowDialogInput() {
        dialogNavigation.activate(
            configuration = DialogConfig(
                dialogState = DialogState.DIALOG_INPUT,
            )
        )
    }

    override fun runGame(
        gameId: Long,
        gameTitle: String,
        gameDirUri: Uri,
        gameFileUri: Uri
    ) {
        val serviceTransExtra = Bundle().apply {
            putLong("gameId", gameId)
            putString("gameTitle", gameTitle)
            putParcelable("gameDirUri", gameDirUri)
            putParcelable("gameFileUri", gameFileUri)
        }

        supervisorService.startService(serviceTransExtra)

        rootDir = gameDirUri.toDocumentFile(get())

        store.accept(RootStore.Intent.StartGameDialogFlow(rootDir))
    }

    override fun restartGame() {
        supervisorService.onRestartGame()
    }

    @OptIn(ExperimentalContracts::class)
    fun getSavesDir(): DocumentFile? {
        // (slot + 1) + ".sav"
        val dir = rootDir
        return if (dir.isWritableDir(get())) {
            dir.makeFolder(get(), "saves", CreateMode.REUSE)
        } else {
            null
        }
    }

    private inner class GameWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uriScheme = request.url.scheme
            if (uriScheme == null) return false
            val uriDecode = Uri.decode(request.url.toString())

            when (uriScheme) {
                "exec" ->
                    uriDecode.substring(5)
                        .let {
                            if (it.isBase64()) {
                                it.decodeBase64()
                            } else {
                                it
                            }
                        }
                        .let {
                            if (it.isContainsHtmlTags()) {
                                store.accept(RootStore.Intent.OnExecCode(it.removeHtmlTags()))
                            } else {
                                store.accept(RootStore.Intent.OnExecCode(it))
                            }
                        }

                "https", "http" ->
                    Intent(
                        Intent.ACTION_VIEW,
                        uriDecode.toUri()
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }.also {
                        try {
                            onStartActivity(it)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(javaClass.simpleName, "shouldOverrideUrlLoading: ERROR!", e)
                        }
                    }

                "file" ->
                    Intent(
                        Intent.ACTION_VIEW,
                        uriScheme.replace("file:/", "https:").toUri()
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }.also {
                        try {
                            onStartActivity(it)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(javaClass.simpleName, "shouldOverrideUrlLoading: ERROR!", e)
                        }
                    }
            }
            return true
        }

        @OptIn(ExperimentalContracts::class)
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val uriScheme = request.url.scheme ?: return super.shouldInterceptRequest(view, request)
            if (!uriScheme.startsWith("file")) return super.shouldInterceptRequest(view, request)
            if (!rootDir.isWritableDir(get())) return super.shouldInterceptRequest(view, request)

            try {
                val uriPath = request.url.path ?: throw NullPointerException()
                val imageFile = rootDir?.child(get(), uriPath)
                if (imageFile.isWritableFile(get())) {
                    return WebResourceResponse(
                        getMimeTypeFromFileName(imageFile.name),
                        null,
                        get<Context>().contentResolver.openInputStream(imageFile.uri)
                    )
                } else {
                    throw FileNotFoundException()
                }
            } catch (ex: Exception) {
                when (ex) {
                    is NullPointerException,
                    is FileNotFoundException -> {
//                        if (getSettingsController().isUseImageDebug) {
//                            doShowErrorDialog(uri.getPath(), ErrorType.IMAGE_ERROR)
//                        }
                        Log.e(javaClass.simpleName, "shouldInterceptRequest: ERROR!", ex)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }


    private fun createChild(
        config: ChildConfig,
        componentContext: ComponentContext
    ): RootComponent.Child = when (config) {
        is ChildConfig.Main -> {
            RootComponent.Child.MainChild(
                RealMainComponent(
                    componentContext = componentContext,
                    gameWebViewClient = GameWebViewClient(),
                )
            )
        }

        is ChildConfig.Extra -> {
            RootComponent.Child.ExtraChild(
                RealExtraComponent(
                    componentContext = componentContext,
                    gameWebViewClient = GameWebViewClient(),
                )
            )
        }

        is ChildConfig.Object -> {
            RootComponent.Child.ObjectChild(
                RealObjectComponent(
                    componentContext = componentContext
                )
            )
        }
    }

    @Serializable
    sealed class ChildConfig {
        @Serializable
        data object Main : ChildConfig()

        @Serializable
        data object Extra : ChildConfig()

        @Serializable
        data object Object : ChildConfig()
    }
}
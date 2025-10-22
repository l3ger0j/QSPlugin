package com.pixnpunk.qsplugin.mvi

import android.content.Context
import android.content.Intent
import com.anggrayudi.storage.file.MimeType.BINARY_FILE
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.pixnpunk.natives.SupervisorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import com.pixnpunk.dialogs.presentation.toDialogState
import com.pixnpunk.dto.LibReturnValue
import com.pixnpunk.dto.LibTypeDialog
import com.pixnpunk.dto.LibTypePopup
import com.pixnpunk.dto.LibTypeWindow
import com.pixnpunk.qsplugin.mvi.RootStore.Label.ShowDialogDefault
import com.pixnpunk.qsplugin.mvi.RootStore.Label.ShowDialogMenu
import com.pixnpunk.qsplugin.mvi.RootStore.Label.ShowDialogMessage
import com.pixnpunk.qsplugin.mvi.RootStore.Label.ShowLoadFilePicker
import com.pixnpunk.qsplugin.mvi.RootStore.Label.ShowSaveFilePicker
import com.pixnpunk.qsplugin.mvi.RootStore.Message.UpdateGameStatus
import com.pixnpunk.qsplugin.mvi.RootStore.Message.UpdateVisExtraElement
import com.pixnpunk.qsplugin.mvi.RootStore.Message.UpdateVisInputElement
import com.pixnpunk.qsplugin.mvi.RootStore.Message.UpdateVisObjElement
import com.pixnpunk.settings.SettingsRepo
import com.pixnpunk.utils.PathUtil.getImageUriFromPath
import com.pixnpunk.utils.PathUtil.normalizeContentPath
import kotlinx.coroutines.flow.filter

internal class RootExecutor(
    private val appContext: Context,
    private val service: SupervisorViewModel,
    private val settingsRepo: SettingsRepo
) : CoroutineExecutor<RootStore.Intent, RootStore.Action, RootStore.State, RootStore.Message, RootStore.Label>() {
    private val outScope = CoroutineScope(Dispatchers.Default)

    override fun executeIntent(intent: RootStore.Intent) {
        when (intent) {
            is RootStore.Intent.CreateLoadIntent -> {
                publish(ShowLoadFilePicker(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.ACTION_GET_CONTENT, true)
                        setType(BINARY_FILE)
                    }
                ))
            }

            is RootStore.Intent.CreateSaveIntent -> {
                publish(ShowSaveFilePicker(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(
                            Intent.EXTRA_TITLE,
                            "${(UInt.MIN_VALUE..UInt.MAX_VALUE).random()}.sav"
                        )
                        setType(BINARY_FILE)
                    }
                ))
            }

            is RootStore.Intent.OnLoadFile -> {
                outScope.launch {
                    service.onLoadFile(intent.fileUri)
                }
            }

            is RootStore.Intent.OnSaveFile -> {
                outScope.launch {
                    service.onSaveFile(intent.fileUri)
                }
            }

            is RootStore.Intent.OnExecCode -> {
                outScope.launch {
                    service.onCodeExec(intent.codeToExec)
                }
            }

            is RootStore.Intent.OnSelectMenuItem -> {
                service.putReturnValue(
                    LibReturnValue(dialogNumValue = intent.index)
                )
            }

            is RootStore.Intent.OnEnterValue -> {
                if (intent.isBox) {
                    service.putReturnValue(
                        LibReturnValue(dialogTextValue = intent.inputString)
                    )
                } else {
                    service.onUseInputArea(inputString = intent.inputString)
                }
            }

            is RootStore.Intent.StartGameDialogFlow -> {
                outScope.launch {
                    service.gameDialogFlow.collect { items ->
                        val payload = if (items.first == LibTypeDialog.DIALOG_PICTURE) {
                            items.second
                                .normalizeContentPath()
                                .getImageUriFromPath(appContext, intent.rootDir)
                                .toString()
                        } else {
                            items.second
                        }
                        when (items.first) {
                            LibTypeDialog.DIALOG_MESSAGE -> {
                                scope.launch {
                                    publish(ShowDialogMessage(payload))
                                }
                            }

                            LibTypeDialog.DIALOG_MENU -> {
                                scope.launch {
                                    publish(ShowDialogMenu(payload, items.third))
                                }
                            }

                            else -> {
                                scope.launch {
                                    publish(
                                        ShowDialogDefault(
                                            payload,
                                            items.first.toDialogState()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }


        }
    }

    override fun executeAction(action: RootStore.Action) {
        when (action) {
            is RootStore.Action.StartGameStateFlow -> {
                outScope.launch {
                    service.gameStateFlow.collect { state ->
                        scope.launch {
                            dispatch(UpdateGameStatus(state.gameRunning))
                        }
                    }
                }
            }

            is RootStore.Action.StartGamePopupFlow -> {
                outScope.launch {
                    service.gamePopupFlow.collect { type ->
                        when (type) {
                            LibTypePopup.POPUP_SAVE -> {
                                scope.launch {
                                    publish(ShowSaveFilePicker(
                                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            putExtra(
                                                Intent.EXTRA_TITLE,
                                                "${(UInt.MIN_VALUE..UInt.MAX_VALUE).random()}.sav"
                                            )
                                            setType(BINARY_FILE)
                                        }
                                    ))
                                }
                            }

                            LibTypePopup.POPUP_LOAD -> {
                                scope.launch {
                                    publish(ShowLoadFilePicker(
                                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            putExtra(Intent.ACTION_GET_CONTENT, true)
                                            setType(BINARY_FILE)
                                        }
                                    ))
                                }
                            }
                        }
                    }
                }
            }

            is RootStore.Action.StartGameElementVisFlow -> {
                outScope.launch {
                    service.gameElementVis
                        .filterNot { (window, _) -> window == LibTypeWindow.ACTS }
                        .collect { (window, bool) ->
                            when (window) {
                                LibTypeWindow.OBJS -> {
                                    scope.launch {
                                        dispatch(UpdateVisObjElement(bool))
                                    }
                                }

                                LibTypeWindow.VARS -> {
                                    scope.launch {
                                        dispatch(UpdateVisExtraElement(bool))
                                    }
                                }

                                LibTypeWindow.INPUT -> {
                                    scope.launch {
                                        dispatch(UpdateVisInputElement(bool))
                                    }
                                }

                                else -> {
                                    // do nothing
                                }
                            }
                        }
                }
            }

            is RootStore.Action.StartGameSettingsFlow -> {
                outScope.launch {
                    settingsRepo.settingsState
                        .filter { it.isUseExecString }
                        .collect { dispatch(RootStore.Message.UpdateExecutorStatus(it.isUseExecString)) }
                }
            }
        }
    }

    override fun dispose() {
        super.dispose()
        outScope.cancel()
    }
}
package org.qp.qsplugin.mvi

import android.content.Context
import android.content.Intent
import com.anggrayudi.storage.file.MimeType.BINARY_FILE
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import org.qp.dialogs.presentation.toDialogState
import org.qp.dto.LibReturnValue
import org.qp.dto.LibTypeDialog
import org.qp.dto.LibTypePopup
import org.qp.dto.LibTypeWindow
import org.qp.qsplugin.mvi.RootStore.Label.*
import org.qp.qsplugin.mvi.RootStore.Message.UpdateGameStatus
import org.qp.qsplugin.mvi.RootStore.Message.UpdateStateNestedLoad
import org.qp.qsplugin.mvi.RootStore.Message.UpdateStateNestedSave
import org.qp.qsplugin.mvi.RootStore.Message.UpdateVisExtraElement
import org.qp.qsplugin.mvi.RootStore.Message.UpdateVisInputElement
import org.qp.qsplugin.mvi.RootStore.Message.UpdateVisObjElement
import org.qp.supervisor.SupervisorService
import org.qp.utils.PathUtil.getImageUriFromPath
import org.qp.utils.PathUtil.normalizeContentPath

internal class RootExecutor(
    private val appContext: Context,
    private val service: SupervisorService
) : CoroutineExecutor<RootStore.Intent, RootStore.Action, RootStore.State, RootStore.Message, RootStore.Label>() {
    private val outScope = CoroutineScope(Dispatchers.Default)

    override fun executeIntent(intent: RootStore.Intent) {
        when (intent) {
            is RootStore.Intent.ChangeStateNestedLoad -> {
                dispatch(UpdateStateNestedLoad(intent.newState))
            }

            is RootStore.Intent.ChangeStateNestedSave -> {
                dispatch(UpdateStateNestedSave(intent.newState))
            }

            is RootStore.Intent.CreateLoadIntent -> {
                publish(ShowLoadFileActivity(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.ACTION_GET_CONTENT, true)
                        setType(BINARY_FILE)
                    }
                ))
            }

            is RootStore.Intent.CreateSaveIntent -> {
                publish(ShowSaveFileActivity(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.EXTRA_TITLE, "${(UInt.MIN_VALUE..UInt.MAX_VALUE).random()}.sav")
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
                outScope.launch {
                    service.returnDialogFlow.emit(
                        LibReturnValue(dialogNumValue = intent.index)
                    )
                }
            }

            is RootStore.Intent.OnEnterValue -> {
                outScope.launch {
                    service.returnDialogFlow.emit(
                        LibReturnValue(dialogTextValue = intent.inputString)
                    )
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
                                    publish(ShowSaveFileActivity(
                                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            putExtra(
                                                Intent.EXTRA_TITLE,
                                                "${(UInt.MIN_VALUE..UInt.MAX_VALUE).random()}.sav"
                                            )
                                            setType(BINARY_FILE)
                                        }
                                    ))
//                                    dispatch(UpdateStateNestedSave(true))
                                }
                            }

                            LibTypePopup.POPUP_LOAD -> {
                                scope.launch {
                                    publish(ShowLoadFileActivity(
                                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            putExtra(Intent.ACTION_GET_CONTENT, true)
                                            setType(BINARY_FILE)
                                        }
                                    ))
//                                    dispatch(UpdateStateNestedLoad(true))
                                }
                            }
                        }
                    }
                }
            }

            is RootStore.Action.StartGameElementVisFlow -> {
                outScope.launch {
                    service.gameElementVis
                        .filterNot { (window, _) -> window == LibTypeWindow.ACTIONS }
                        .collect { (window, bool) ->
                            when (window) {
                                LibTypeWindow.OBJECTS -> {
                                    scope.launch {
                                        dispatch(UpdateVisObjElement(bool))
                                    }
                                }

                                LibTypeWindow.VARIABLES -> {
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
        }
    }

    override fun dispose() {
        super.dispose()
        outScope.cancel()
    }
}
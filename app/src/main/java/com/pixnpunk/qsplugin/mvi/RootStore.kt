package com.pixnpunk.qsplugin.mvi

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.arkivanov.mvikotlin.core.store.Store
import kotlinx.serialization.Serializable
import com.pixnpunk.dialogs.presentation.DialogState
import com.pixnpunk.dto.LibGenItem

sealed interface RootStore : Store<RootStore.Intent, RootStore.State, RootStore.Label> {
    @Serializable
    data class State(
        val isObjectsEnabled: Boolean = true,
        val isExtraEnabled: Boolean = true,
        val isInputEnabled: Boolean = true,
        val isGameRunning: Boolean = false,
        val inLoadExpanded: Boolean = false,
        val isSaveExpanded: Boolean = false
    )

    sealed interface Intent {
        data class StartGameDialogFlow(val rootDir: DocumentFile?) : Intent
        data class OnSaveFile(val fileUri: Uri) : Intent
        data class OnLoadFile(val fileUri: Uri) : Intent
        data class OnExecCode(val codeToExec: String) : Intent
        data class OnSelectMenuItem(val index: Int) : Intent
        data class OnEnterValue(val inputString: String) : Intent
        data object CreateSaveIntent : Intent
        data object CreateLoadIntent : Intent
        data class ChangeStateNestedLoad(val newState: Boolean) : Intent
        data class ChangeStateNestedSave(val newState: Boolean) : Intent
    }

    sealed interface Message {
        data class UpdateGameStatus(val isGameRunning: Boolean) : Message
        data class UpdateVisObjElement(val isObjectsEnabled: Boolean) : Message
        data class UpdateVisExtraElement(val isExtraEnabled: Boolean) : Message
        data class UpdateVisInputElement(val isInputEnabled: Boolean) : Message
        data class UpdateStateNestedLoad(val newState: Boolean) : Message
        data class UpdateStateNestedSave(val newState: Boolean) : Message
    }

    sealed interface Label {
        data class ShowDialogMessage(
            val inputString: String
        ) : Label

        data class ShowDialogMenu(
            val inputString: String,
            val inputListItems: List<LibGenItem>
        ) : Label

        data class ShowDialogDefault(
            val inputString: String,
            val dialogState: DialogState
        ) : Label

        data class ShowSaveFileActivity(
            val intent: android.content.Intent
        ) : Label

        data class ShowLoadFileActivity(
            val intent: android.content.Intent
        ) : Label
    }

    sealed interface Action {
        data object StartGameStateFlow : Action
        data object StartGamePopupFlow : Action
        data object StartGameElementVisFlow : Action
    }
}
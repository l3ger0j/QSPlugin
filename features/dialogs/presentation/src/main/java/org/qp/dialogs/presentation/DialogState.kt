package org.qp.dialogs.presentation

import org.qp.dialogs.presentation.DialogState.DIALOG_ERROR
import org.qp.dialogs.presentation.DialogState.DIALOG_INPUT
import org.qp.dialogs.presentation.DialogState.DIALOG_MENU
import org.qp.dialogs.presentation.DialogState.DIALOG_MESSAGE
import org.qp.dialogs.presentation.DialogState.DIALOG_PICTURE
import org.qp.dto.LibTypeDialog

enum class DialogState {
    DIALOG_ERROR,
    DIALOG_PICTURE,
    DIALOG_MESSAGE,
    DIALOG_INPUT,
    DIALOG_MENU,
}

fun LibTypeDialog.toDialogState(): DialogState {
    return when (this) {
        LibTypeDialog.DIALOG_ERROR -> DIALOG_ERROR
        LibTypeDialog.DIALOG_PICTURE -> DIALOG_PICTURE
        LibTypeDialog.DIALOG_MESSAGE -> DIALOG_MESSAGE
        LibTypeDialog.DIALOG_INPUT -> DIALOG_INPUT
        LibTypeDialog.DIALOG_MENU -> DIALOG_MENU
    }
}
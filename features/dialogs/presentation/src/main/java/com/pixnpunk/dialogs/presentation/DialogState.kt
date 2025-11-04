package com.pixnpunk.dialogs.presentation

import com.pixnpunk.dto.LibTypeDialog

enum class DialogState {
    DIALOG_ERROR,
    DIALOG_PERMISSION,
    DIALOG_PICTURE,
    DIALOG_MESSAGE,
    DIALOG_INPUT,
    DIALOG_MENU,
}

fun LibTypeDialog.toDialogState(): DialogState {
    return when (this) {
        LibTypeDialog.DIALOG_ERROR -> DialogState.DIALOG_ERROR
        LibTypeDialog.DIALOG_PICTURE -> DialogState.DIALOG_PICTURE
        LibTypeDialog.DIALOG_MESSAGE -> DialogState.DIALOG_MESSAGE
        LibTypeDialog.DIALOG_INPUT -> DialogState.DIALOG_INPUT
        LibTypeDialog.DIALOG_MENU -> DialogState.DIALOG_MENU
    }
}
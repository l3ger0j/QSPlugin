package com.pixnpunk.dialogs.presentation

import kotlinx.serialization.Serializable
import com.pixnpunk.dto.LibGenItem

@Serializable
data class DialogConfig(
    val dialogState: DialogState,
    val dialogInputString: String = "",
    val isDialogInputBox: Boolean = false,
    val isDialogInputError: Boolean = false,
    val dialogInputErrorString: String = "",
    val dialogMenuItems: List<LibGenItem> = emptyList()
)
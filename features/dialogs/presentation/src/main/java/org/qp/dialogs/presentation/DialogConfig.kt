package org.qp.dialogs.presentation

import kotlinx.serialization.Serializable
import org.qp.dto.LibGenItem

@Serializable
data class DialogConfig(
    val dialogState: DialogState,
    val dialogInputString: String,
    val isDialogInputError: Boolean = false,
    val dialogInputErrorString: String = "",
    val dialogMenuItems: List<LibGenItem> = emptyList()
)
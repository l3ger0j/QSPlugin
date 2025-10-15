package com.pixnpunk.dialogs.presentation

import kotlinx.coroutines.flow.StateFlow
import com.pixnpunk.dto.GameSettings

sealed interface DialogsComponent {
    val settingsFlow: StateFlow<GameSettings>
    val dialogConfig: DialogConfig
    val onCleanError: () -> Unit
    val onEnterValue: (Pair<String, Boolean>) -> Unit
    val onSelMenuItem: (Int) -> Unit
    val onDismissed: () -> Unit
}
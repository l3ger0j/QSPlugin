package org.qp.dialogs.presentation

import kotlinx.coroutines.flow.StateFlow
import org.qp.dto.GameSettings

sealed interface DialogsComponent {
    val settingsFlow: StateFlow<GameSettings>

    val dialogConfig: DialogConfig
    val onCleanError: () -> Unit
    val onEnterValue: (String) -> Unit
    val onSelMenuItem: (Int) -> Unit
    val onDismissed: () -> Unit
}
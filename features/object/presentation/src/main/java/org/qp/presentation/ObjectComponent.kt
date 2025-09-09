package org.qp.presentation

import kotlinx.coroutines.flow.StateFlow
import org.qp.dto.GameSettings
import org.qp.presentation.mvi.ObjectStore

interface ObjectComponent {
    val model: StateFlow<ObjectStore.State>
    val settingsFlow: StateFlow<GameSettings>

    val onObjectSelected: (Int) -> Unit
}
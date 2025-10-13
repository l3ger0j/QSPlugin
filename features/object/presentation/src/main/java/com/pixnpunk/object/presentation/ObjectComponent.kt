package com.pixnpunk.`object`.presentation

import kotlinx.coroutines.flow.StateFlow
import com.pixnpunk.dto.GameSettings
import com.pixnpunk.`object`.presentation.mvi.ObjectStore

interface ObjectComponent {
    val model: StateFlow<ObjectStore.State>
    val settingsFlow: StateFlow<GameSettings>

    val onObjectSelected: (Int) -> Unit
}
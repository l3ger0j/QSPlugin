package com.pixnpunk.main.presentation

import android.webkit.WebView
import kotlinx.coroutines.flow.StateFlow
import com.pixnpunk.main.presentation.mvi.MainStore
import com.pixnpunk.dto.GameSettings

sealed interface MainComponent {
    val model: StateFlow<MainStore.State>
    val settingsFlow: StateFlow<GameSettings>

    val onActionClicked: (Int) -> Unit
    fun getDefaultWebClient(view: WebView): WebView
}
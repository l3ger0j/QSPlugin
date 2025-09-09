package org.qp.main.presentation

import android.webkit.WebView
import kotlinx.coroutines.flow.StateFlow
import org.qp.main.presentation.mvi.MainStore
import org.qp.dto.GameSettings

sealed interface MainComponent {
    val model: StateFlow<MainStore.State>
    val settingsFlow: StateFlow<GameSettings>

    val onActionClicked: (Int) -> Unit
    fun getDefaultWebClient(view: WebView): WebView
}
package org.qp.extra.presentation

import android.webkit.WebView
import kotlinx.coroutines.flow.StateFlow
import org.qp.dto.GameSettings
import org.qp.extra.presentation.mvi.ExtraStore

interface ExtraComponent {
    val model: StateFlow<ExtraStore.State>
    val settingsFlow: StateFlow<GameSettings>

    fun getDefaultWebClient(view: WebView): WebView
}
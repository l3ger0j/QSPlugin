package com.pixnpunk.extra.presentation

import android.webkit.WebView
import kotlinx.coroutines.flow.StateFlow
import com.pixnpunk.dto.GameSettings
import com.pixnpunk.extra.presentation.mvi.ExtraStore

interface ExtraComponent {
    val model: StateFlow<ExtraStore.State>
    val settingsFlow: StateFlow<GameSettings>

    fun getDefaultWebClient(view: WebView): WebView
}
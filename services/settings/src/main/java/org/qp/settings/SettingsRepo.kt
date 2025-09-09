package org.qp.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.qp.dto.GameSettings
import org.qp.supervisor.SupervisorService

class SettingsRepo : ViewModel(), KoinComponent {

    private val context: Context by inject()
    private val service: SupervisorService by inject()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var gameConfJob: Job? = null
    private val _settingsState = MutableStateFlow(GameSettings())

    private val applicationSettingsReceiver = ApplicationSettingsReceiver()

    val settingsState: StateFlow<GameSettings> = _settingsState

    fun emitValue(settings: GameSettings) {
        scope.launch {
            _settingsState.emit(settings)
        }
    }

    private inner class ApplicationSettingsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "org.qp.settings.ACTION_SETTINGS_UPDATE") {
                val settings = intent.getParcelableExtra<GameSettings>("qapplicationSettings")
                if (settings != null) {
                    scope.launch {
                        _settingsState.emit(settings)
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter("org.qp.settings.ACTION_SETTINGS_UPDATE")
        ContextCompat.registerReceiver(
            context,
            applicationSettingsReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        gameConfJob = scope.launch {
            _settingsState.combine(service.gameUIConfFlow) { appSet, libSet ->
                appSet.copy(
                    isUseHtml = libSet.useHtml,
                    textColor = if (appSet.isUseGameTextColor && libSet.fontColor != 0) { libSet.fontColor } else { appSet.textColor },
                    backColor = if (appSet.isUseGameBackgroundColor && libSet.backColor != 0) { libSet.backColor } else { appSet.backColor },
                    linkColor = if (appSet.isUseGameLinkColor && libSet.linkColor != 0) { libSet.linkColor } else { appSet.linkColor },
                    fontSize = if (appSet.isUseGameFont && libSet.fontSize != 0) { libSet.fontSize } else { appSet.fontSize }
                )
            }.collect(_settingsState::emit)
        }
    }

    override fun onCleared() {
        gameConfJob?.cancel()
        scope.cancel()
        context.unregisterReceiver(applicationSettingsReceiver)
    }
}

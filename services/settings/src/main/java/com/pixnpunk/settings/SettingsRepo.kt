package com.pixnpunk.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.pixnpunk.dto.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsRepo(
    private val appContext: Context
) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Default)
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
            if (intent.action == "com.pixnpunk.settings.ACTION_SETTINGS_UPDATE") {
                val gameSettingsString = intent.getStringExtra("gameAppSettings")
                if (gameSettingsString != null) {
                    scope.launch {
                        _settingsState.emit(Json.decodeFromString<GameSettings>(gameSettingsString))
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter("org.qp.settings.ACTION_SETTINGS_UPDATE")
        ContextCompat.registerReceiver(
            appContext,
            applicationSettingsReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onCleared() {
        appContext.unregisterReceiver(applicationSettingsReceiver)
        scope.cancel()
    }
}

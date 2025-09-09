package org.qp.qsplugin

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.qp.audio.audioModule
import org.qp.settings.settingsModule
import org.qp.supervisor.supervisorModule

class QSPlugin : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QSPlugin)
            modules(audioModule, supervisorModule, settingsModule)
        }
    }
}
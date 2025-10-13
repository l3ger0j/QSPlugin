package com.pixnpunk.qsplugin

import android.app.Application
import com.pixnpunk.natives.di.supervisorModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.pixnpunk.audio.audioModule
import com.pixnpunk.settings.settingsModule

class QSPlugin : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QSPlugin)
            modules(audioModule, supervisorModule, settingsModule)
        }
    }
}
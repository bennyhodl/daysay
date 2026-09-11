package com.benschroth.daylightmic

import android.app.Application
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.settings.SettingsStore

class DaylightMicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        Dictation.init(this)
    }
}

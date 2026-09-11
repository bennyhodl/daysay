package dev.bennyb.daysay

import android.app.Application
import dev.bennyb.daysay.core.Dictation
import dev.bennyb.daysay.model.TranscriptStore
import dev.bennyb.daysay.settings.SettingsStore

class DaysayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        TranscriptStore.init(this)
        Dictation.init(this)
    }
}

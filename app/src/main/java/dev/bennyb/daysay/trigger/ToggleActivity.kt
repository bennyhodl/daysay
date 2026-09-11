package dev.bennyb.daysay.trigger

import android.app.Activity
import android.os.Bundle
import dev.bennyb.daysay.core.Dictation

/**
 * Invisible entry point for other apps and automation tools such as KeyMapper.
 * Launching it toggles dictation and exits at once.
 */
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dictation.toggle()
        finish()
    }
}

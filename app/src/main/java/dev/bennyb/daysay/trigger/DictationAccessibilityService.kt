package dev.bennyb.daysay.trigger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.InputMethod
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.bennyb.daysay.core.Delivery
import dev.bennyb.daysay.core.Dictation
import dev.bennyb.daysay.core.DictationState
import dev.bennyb.daysay.core.KeySeen
import dev.bennyb.daysay.overlay.Bubble
import dev.bennyb.daysay.settings.SettingsStore
import dev.bennyb.daysay.settings.TriggerKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Does three jobs:
 *  1. Sees every hardware key press before the rest of the system and toggles dictation on the
 *     trigger key.
 *  2. Draws the speech bubble as an accessibility overlay.
 *  3. Acts as a minimal input method so the transcript can be committed straight into the
 *     focused text field.
 */
class DictationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DictationA11y"

        @Volatile
        var instance: DictationAccessibilityService? = null
            private set

        val isRunning: Boolean get() = instance != null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bubble: Bubble? = null
    private var consumedDown = false
    private var learnConsumedDown = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        bubble = Bubble(this, onStop = { Dictation.toggle() }, onCancel = { Dictation.cancel() })
        scope.launch {
            // The panel is for other apps. Inside Daysay the home slab already shows the state.
            combine(Dictation.state, Dictation.appVisible) { state, inApp -> if (inApp) DictationState.Idle else state }
                .collect { bubble?.render(it) }
        }
        Log.i(TAG, "connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        bubble?.destroy()
        bubble = null
        scope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onCreateInputMethod(): InputMethod = object : InputMethod(this) {}

    // ---- Key handling -------------------------------------------------------------------

    /**
     * Runs on the main thread with a 500 ms budget. Returning true consumes the event, so a
     * matched DOWN must always be followed by a consumed UP to keep the key stream well formed.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        Dictation.lastKey.value = KeySeen(event.keyCode, event.scanCode, event.deviceId)

        if (Dictation.learnMode.value) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        val learned = TriggerKey(event.keyCode, event.scanCode)
                        SettingsStore.update { it.copy(trigger = learned) }
                        Dictation.learnMode.value = false
                        learnConsumedDown = true
                        Log.i(TAG, "learned trigger $learned (${KeyEvent.keyCodeToString(event.keyCode)})")
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    val consumed = learnConsumedDown
                    learnConsumedDown = false
                    return consumed
                }
            }
            return false
        }
        if (learnConsumedDown && event.action == KeyEvent.ACTION_UP) {
            learnConsumedDown = false
            return true
        }

        val trigger = SettingsStore.current.trigger
        if (!trigger.matches(event.keyCode, event.scanCode)) return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    consumedDown = true
                    Dictation.toggle()
                }
                true
            }
            KeyEvent.ACTION_UP -> {
                val consumed = consumedDown
                consumedDown = false
                consumed
            }
            else -> false
        }
    }

    // ---- Text delivery ------------------------------------------------------------------

    /**
     * Puts [text] into the focused editable field. Order: input connection, then
     * ACTION_SET_TEXT, then ACTION_PASTE (the clipboard already holds the text).
     * Returns null when no editable field has focus.
     */
    suspend fun insertText(text: String): Delivery? = withContext(Dispatchers.Main) {
        if (insertViaInputConnection(text)) return@withContext Delivery.INSERTED
        val node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return@withContext null
        if (!node.isEditable) return@withContext null
        if (insertViaSetText(node, text)) return@withContext Delivery.INSERTED
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return@withContext Delivery.PASTED
        null
    }

    private fun insertViaInputConnection(text: String): Boolean {
        val ime = inputMethod ?: return false
        if (!ime.currentInputStarted) return false
        val ic = ime.currentInputConnection ?: return false
        return runCatching {
            val before = ic.getSurroundingText(1, 0, 0)?.let { st ->
                st.text.subSequence(0, st.selectionStart.coerceIn(0, st.text.length)).toString()
            }.orEmpty()
            ic.commitText(withLeadingSpace(before, text), 1, null)
            true
        }.getOrElse {
            Log.w(TAG, "commitText failed", it)
            false
        }
    }

    private fun insertViaSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        val existing = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        val selStart = node.textSelectionStart.takeIf { it >= 0 } ?: existing.length
        val selEnd = node.textSelectionEnd.takeIf { it >= 0 } ?: selStart
        val lo = minOf(selStart, selEnd).coerceIn(0, existing.length)
        val hi = maxOf(selStart, selEnd).coerceIn(0, existing.length)
        val before = existing.substring(0, lo)
        val inserted = withLeadingSpace(before, text)
        val merged = before + inserted + existing.substring(hi)

        val setArgs = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged)
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setArgs)) return false

        val caret = lo + inserted.length
        val selArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, caret)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, caret)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
        return true
    }

    /** Adds one space when the text before the cursor ends in a non-space character. */
    private fun withLeadingSpace(before: String, text: String): String =
        if (before.isNotEmpty() && !before.last().isWhitespace()) " $text" else text
}

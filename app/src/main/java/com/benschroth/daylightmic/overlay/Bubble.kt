package com.benschroth.daylightmic.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.benschroth.daylightmic.core.Delivery
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.core.DictationState

/**
 * The floating dictation panel: a waveform on top, a status row underneath.
 * Ink on paper inverted, so it reads as a distinct object on the Daylight display.
 * Drawn as an accessibility overlay: no extra permission, and it never takes focus from the
 * field the user is dictating into.
 */
class Bubble(private val service: AccessibilityService, private val onStop: () -> Unit, private val onCancel: () -> Unit) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val model = WaveformModel()

    private var root: LinearLayout? = null
    private lateinit var wave: WaveformView
    private lateinit var status: TextView
    private lateinit var stopButton: TextView
    private lateinit var cancelButton: TextView

    private var mode = WaveformModel.Mode.IDLE
    private val hideRunnable = Runnable { hide() }
    private val frame = object : Runnable {
        override fun run() {
            model.tick(Dictation.level.value, mode)
            if (::wave.isInitialized) wave.invalidate()
            if (root != null) handler.postDelayed(this, WaveformModel.FRAME_MS)
        }
    }

    fun render(state: DictationState) {
        handler.removeCallbacks(hideRunnable)
        when (state) {
            DictationState.Idle -> hide()
            is DictationState.Listening -> show("Listening", WaveformModel.Mode.LISTENING, stop = true, cancel = true)
            DictationState.Transcribing -> show("Transcribing", WaveformModel.Mode.PROCESSING, stop = false, cancel = true)
            DictationState.Cleaning -> show("Cleaning up", WaveformModel.Mode.PROCESSING, stop = false, cancel = true)
            is DictationState.Done -> {
                val text = when (state.delivery) {
                    Delivery.INSERTED -> "Inserted"
                    Delivery.PASTED -> "Pasted"
                    Delivery.CLIPBOARD -> "Copied to clipboard"
                }
                show(text, WaveformModel.Mode.IDLE, stop = false, cancel = false)
                handler.postDelayed(hideRunnable, 1_400)
            }
            is DictationState.Failed -> {
                show(state.message, WaveformModel.Mode.IDLE, stop = false, cancel = false)
                handler.postDelayed(hideRunnable, 4_000)
            }
        }
    }

    private fun show(text: String, newMode: WaveformModel.Mode, stop: Boolean, cancel: Boolean) {
        val wasHidden = root == null
        val view = root ?: build().also { attach(it) }
        if (wasHidden) {
            model.reset()
            handler.removeCallbacks(frame)
            handler.post(frame)
        }
        mode = newMode
        status.text = text
        stopButton.visibility = if (stop) View.VISIBLE else View.GONE
        cancelButton.visibility = if (cancel) View.VISIBLE else View.GONE
        view.contentDescription = text
    }

    private fun hide() {
        handler.removeCallbacks(frame)
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        mode = WaveformModel.Mode.IDLE
    }

    fun destroy() {
        handler.removeCallbacks(hideRunnable)
        hide()
    }

    private fun build(): LinearLayout {
        wave = WaveformView(service, model).apply { barColor = PAPER }
        status = label(PAPER, 15f)
        stopButton = chip("Stop").apply { setOnClickListener { onStop() } }
        cancelButton = chip("Cancel").apply { setOnClickListener { onCancel() } }

        val bottomRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(12), dp(12))
            addView(status, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(stopButton, chipParams())
            addView(cancelButton, chipParams())
        }

        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(INK)
            }
            addView(wave, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply {
                setMargins(dp(20), dp(18), dp(20), 0)
            })
            addView(bottomRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun label(color: Int, sizeSp: Float): TextView = TextView(service).apply {
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        typeface = Typeface.SANS_SERIF
        maxLines = 2
    }

    private fun chip(text: String): TextView = label(PAPER, 14f).apply {
        this.text = text
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(1), ASH)
        }
    }

    private fun chipParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { marginStart = dp(8) }

    private fun attach(view: LinearLayout) {
        val params = WindowManager.LayoutParams(
            dp(380),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(40)
        }
        wm.addView(view, params)
        root = view
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), service.resources.displayMetrics).toInt()

    companion object {
        private val INK = Color.parseColor("#141414")
        private val PAPER = Color.parseColor("#F3EEE4")
        private val ASH = Color.parseColor("#6E6E6E")
    }
}

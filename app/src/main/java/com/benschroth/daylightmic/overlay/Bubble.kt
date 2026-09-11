package com.benschroth.daylightmic.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PixelFormat
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
import com.benschroth.daylightmic.core.DictationState

/**
 * The small speech bubble. Drawn as an accessibility overlay, so it needs no extra permission
 * and never takes focus away from the text field the user is dictating into.
 * Grayscale only: the Daylight display has no colour.
 */
class Bubble(private val service: AccessibilityService, private val onTap: () -> Unit) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private var root: LinearLayout? = null
    private lateinit var dot: View
    private lateinit var label: TextView
    private var pulse: ObjectAnimator? = null
    private val hideRunnable = Runnable { hide() }

    fun render(state: DictationState) {
        handler.removeCallbacks(hideRunnable)
        when (state) {
            DictationState.Idle -> hide()
            is DictationState.Listening -> show("Listening. Tap to cancel", pulsing = true)
            DictationState.Transcribing -> show("Transcribing", pulsing = false)
            DictationState.Cleaning -> show("Cleaning up", pulsing = false)
            is DictationState.Done -> {
                val text = when (state.delivery) {
                    Delivery.INSERTED -> "Inserted"
                    Delivery.PASTED -> "Pasted"
                    Delivery.CLIPBOARD -> "Copied to clipboard"
                }
                show(text, pulsing = false)
                handler.postDelayed(hideRunnable, 1_500)
            }
            is DictationState.Failed -> {
                show(state.message, pulsing = false)
                handler.postDelayed(hideRunnable, 4_000)
            }
        }
    }

    private fun show(text: String, pulsing: Boolean) {
        val view = root ?: build().also { attach(it) }
        label.text = text
        if (pulsing) startPulse() else stopPulse()
        view.contentDescription = text
    }

    private fun hide() {
        stopPulse()
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    fun destroy() {
        handler.removeCallbacks(hideRunnable)
        hide()
    }

    private fun build(): LinearLayout {
        val paper = Color.parseColor("#F3EEE4")
        val ink = Color.parseColor("#111111")

        dot = View(service).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ink)
            }
        }
        label = TextView(service).apply {
            setTextColor(ink)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            maxLines = 2
            maxWidth = dp(320)
        }
        return LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(18), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(paper)
                setStroke(dp(2), ink)
            }
            addView(dot, LinearLayout.LayoutParams(dp(12), dp(12)).apply { marginEnd = dp(12) })
            addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            setOnClickListener { onTap() }
        }
    }

    private fun attach(view: LinearLayout) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48)
        }
        wm.addView(view, params)
        root = view
    }

    private fun startPulse() {
        if (pulse != null) return
        pulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.25f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopPulse() {
        pulse?.cancel()
        pulse = null
        if (::dot.isInitialized) dot.alpha = 1f
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), service.resources.displayMetrics).toInt()
}

package com.benschroth.daylightmic.overlay

import kotlin.math.PI
import kotlin.math.sin

/**
 * Bar heights for the waveform, shared by the overlay View and the Compose hero.
 * Listening: the newest level enters on the right and older bars scroll left.
 * Processing: a slow wave travels across the bars so the display reads as "working".
 * Idle: bars settle to a thin baseline.
 */
class WaveformModel(val barCount: Int = 48) {
    enum class Mode { IDLE, LISTENING, PROCESSING }

    private val target = FloatArray(barCount)
    val heights = FloatArray(barCount) { BASELINE }
    private var phase = 0f

    /** Advances one frame. Call about 20 times a second. */
    fun tick(level: Float, mode: Mode) {
        when (mode) {
            Mode.LISTENING -> {
                System.arraycopy(target, 1, target, 0, barCount - 1)
                target[barCount - 1] = BASELINE + level.coerceIn(0f, 1f) * (1f - BASELINE)
            }
            Mode.PROCESSING -> {
                phase += 0.22f
                for (i in 0 until barCount) {
                    val x = i.toFloat() / barCount
                    val envelope = 0.5f - 0.5f * kotlin.math.cos((x * 2 * PI).toFloat())
                    val wave = sin(x * 6f * PI.toFloat() - phase)
                    target[i] = BASELINE + (0.12f + 0.28f * envelope) * (0.5f + 0.5f * wave)
                }
            }
            Mode.IDLE -> for (i in 0 until barCount) target[i] = BASELINE
        }
        for (i in 0 until barCount) {
            heights[i] += (target[i] - heights[i]) * SMOOTHING
        }
    }

    fun reset() {
        target.fill(BASELINE)
        heights.fill(BASELINE)
        phase = 0f
    }

    companion object {
        const val BASELINE = 0.06f
        private const val SMOOTHING = 0.55f
        const val FRAME_MS = 50L
    }
}

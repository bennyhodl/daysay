package dev.bennyb.daysay.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Draws a [WaveformModel] as symmetric rounded bars, mirrored around the vertical centre. */
class WaveformView(context: Context, private val model: WaveformModel) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }
    var barColor: Int = 0xFFF3EEE4.toInt()
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val n = model.barCount
        val w = width.toFloat()
        val h = height.toFloat()
        val slot = w / n
        val barW = slot * 0.5f
        val centerY = h / 2f
        val maxHalf = h / 2f
        paint.color = barColor
        for (i in 0 until n) {
            val half = (model.heights[i] * maxHalf).coerceAtLeast(barW / 2f)
            val left = i * slot + (slot - barW) / 2f
            canvas.drawRoundRect(left, centerY - half, left + barW, centerY + half, barW / 2f, barW / 2f, paint)
        }
    }
}

package com.benschroth.daylightmic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.core.DictationState
import com.benschroth.daylightmic.overlay.WaveformModel
import kotlinx.coroutines.delay

/** The same bars as the floating panel, drawn with Compose. */
@Composable
fun Waveform(state: DictationState, modifier: Modifier = Modifier, color: Color = Paper.paper) {
    val model = remember { WaveformModel(barCount = 56) }
    var frame by remember { mutableIntStateOf(0) }
    val mode = when (state) {
        is DictationState.Listening -> WaveformModel.Mode.LISTENING
        DictationState.Transcribing, DictationState.Cleaning -> WaveformModel.Mode.PROCESSING
        else -> WaveformModel.Mode.IDLE
    }
    LaunchedEffect(mode) {
        while (true) {
            model.tick(Dictation.level.value, mode)
            frame++
            delay(WaveformModel.FRAME_MS)
        }
    }
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION") frame
        val n = model.barCount
        val slot = size.width / n
        val barW = slot * 0.5f
        val centerY = size.height / 2f
        for (i in 0 until n) {
            val half = (model.heights[i] * centerY).coerceAtLeast(barW / 2f)
            val left = i * slot + (slot - barW) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(left, centerY - half),
                size = Size(barW, half * 2f),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}

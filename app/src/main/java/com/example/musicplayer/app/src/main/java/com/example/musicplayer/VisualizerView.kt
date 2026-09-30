package com.example.musicplayer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun VisualizerView(
    fftData: FloatArray?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val data = fftData ?: return@Canvas
        if (data.isEmpty()) return@Canvas

        val bars = 48
        val barWidth = size.width / bars
        val centerY = size.height / 2
        val maxMag = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f

        for (i in 0 until bars) {
            val index = (i * data.size / bars).coerceIn(0, data.size - 1)
            val mag = data[index]
            val normalized = (kotlin.math.ln(1f + mag) / kotlin.math.ln(1f + maxMag))
                .coerceIn(0f, 1f)
            val barHeight = (centerY * 0.85f) * normalized
            val x = i * barWidth + barWidth / 2

            drawLine(
                color = color.copy(alpha = 0.5f + normalized * 0.5f),
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth * 0.55f
            )
        }

        val path = Path()
        for (i in 0 until bars) {
            val index = (i * data.size / bars).coerceIn(0, data.size - 1)
            val mag = data[index]
            val normalized = (kotlin.math.ln(1f + mag) / kotlin.math.ln(1f + maxMag))
                .coerceIn(0f, 1f)
            val x = i * barWidth + barWidth / 2
            val y = centerY - (centerY * 0.85f) * normalized
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3f))
    }
}

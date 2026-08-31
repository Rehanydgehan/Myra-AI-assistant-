package com.myra.assistant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.myra.assistant.ui.theme.PrimaryRed
import com.myra.assistant.ui.theme.Purple
import kotlinx.coroutines.delay

@Composable
fun WaveformView(rms: Float, modifier: Modifier = Modifier) {
    val barCount = 20
    var barHeights by remember { mutableStateOf(FloatArray(barCount) { 0.1f }) }

    LaunchedEffect(rms) {
        while (true) {
            val newHeights = FloatArray(barCount)
            for (i in 0 until barCount) {
                val target = if (rms > 0.05f) (Math.random().toFloat() * rms).coerceIn(0.1f, 1f) else 0.15f
                newHeights[i] = barHeights[i] + (target - barHeights[i]) * 0.3f
            }
            barHeights = newHeights
            delay(50)
        }
    }

    // Colors mapping to match HTML design approximately
    val barColors = listOf(
        PrimaryRed.copy(alpha = 0.4f), PrimaryRed.copy(alpha = 0.5f), PrimaryRed.copy(alpha = 0.7f),
        Purple, Purple, PrimaryRed, PrimaryRed, PrimaryRed.copy(alpha = 0.6f),
        Purple, Purple, PrimaryRed.copy(alpha = 0.5f), PrimaryRed.copy(alpha = 0.4f),
        PrimaryRed.copy(alpha = 0.7f), Purple, Purple, PrimaryRed.copy(alpha = 0.6f),
        PrimaryRed.copy(alpha = 0.4f), PrimaryRed.copy(alpha = 0.7f), Purple, Purple.copy(alpha = 0.5f)
    )

    Canvas(modifier = modifier.width(200.dp).height(48.dp)) {
        val totalWidth = size.width
        val barWidth = 6.dp.toPx()
        val gap = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)
        val maxHeight = size.height
        
        for (i in 0 until barCount) {
            val h = barHeights[i] * maxHeight
            val x = i * (barWidth + gap) + (barWidth / 2f)
            val color = barColors.getOrElse(i) { PrimaryRed }
            
            // Draw from bottom up
            drawLine(
                color = color,
                start = Offset(x, maxHeight),
                end = Offset(x, maxHeight - h.coerceAtLeast(barWidth)), // Min height is a circle
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

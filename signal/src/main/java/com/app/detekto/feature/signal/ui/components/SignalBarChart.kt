package com.app.detekto.feature.signal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.app.detekto.core.theme.ChartBarColors
import com.app.detekto.feature.signal.domain.model.SignalInfo
import com.app.detekto.feature.signal.domain.model.networkGeneration

@Composable
fun SignalBarChart(
    signals: List<SignalInfo>,
    modifier: Modifier = Modifier
) {
    val barColors = ChartBarColors

    val barHeightDp = 32
    val spacingDp = 16
    val verticalPaddingDp = 12
    val chartHeight = signals.size * (barHeightDp + spacingDp) - spacingDp + verticalPaddingDp * 2

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight.dp)
            .padding(horizontal = 16.dp)
    ) {
        val canvasWidth = size.width
        val labelWidth = canvasWidth * 0.35f
        val barAreaWidth = canvasWidth - labelWidth - 60f
        val barHeight = barHeightDp.dp.toPx()
        val spacing = spacingDp.dp.toPx()
        val startY = verticalPaddingDp.dp.toPx()

        signals.forEachIndexed { index, signal ->
            val color = barColors[index % barColors.size]
            val y = startY + index * (barHeight + spacing)
            val barWidth = (signal.signalStrengthPercent / 100f) * barAreaWidth
            val gen = networkGeneration(signal.networkType)
            val label = "${signal.operatorName} $gen"

            // Operator label with network generation (e.g., "Jio 4G")
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textSize = 12.dp.toPx()
                    this.color = android.graphics.Color.DKGRAY
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText(
                    label,
                    0f,
                    y + barHeight / 2 + 5.dp.toPx(),
                    paint
                )
            }

            // Bar background
            drawRoundRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(labelWidth, y),
                size = Size(barAreaWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            // Bar fill
            drawRoundRect(
                color = color,
                topLeft = Offset(labelWidth, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            // Percentage text
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textSize = 12.dp.toPx()
                    this.color = android.graphics.Color.DKGRAY
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText(
                    "${signal.signalStrengthPercent}%",
                    labelWidth + barAreaWidth + 8.dp.toPx(),
                    y + barHeight / 2 + 5.dp.toPx(),
                    paint
                )
            }
        }
    }
}

package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData

@Composable
fun ChartBars(
    modifier: Modifier,
    chartData: ChartData,
) {
    val color = if (chartData.disabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob
    Canvas(
        modifier = modifier,
        onDraw = {
            val barMinHeight = 2.dp.toPx()
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val valuesByTimestamp = chartData.valuesByTimestamp()

            var valueMin = valuesByTimestamp.values.minOrNull() ?: 0f
            val valueMax = valuesByTimestamp.values.maxOrNull() ?: 0f
            if (valueMin == valueMax) {
                valueMin *= 0.9f
            }
            var timestampMin = chartData.startTimestamp
            var timestampMax = chartData.endTimestamp
            if (timestampMin == timestampMax) {
                timestampMin = (timestampMin * 0.9).toLong()
                timestampMax = (timestampMax * 1.1).toLong()
            }

            val xRatio = (canvasWidth - barWidth) / (timestampMax - timestampMin)
            val yRatio = (canvasHeight - barMinHeight) / (valueMax - valueMin)

            scale(scaleX = 1f, scaleY = -1f) {
                valuesByTimestamp.forEach { (timestamp, value) ->
                    val x = (timestamp - timestampMin) * xRatio + barWidth / 2
                    val y = ((value - valueMin) * yRatio + barMinHeight)

                    drawLine(
                        start = Offset(x = x, y = 0f),
                        end = Offset(x = x, y = y),
                        color = color,
                        strokeWidth = barWidth
                    )
                }
            }
        }
    )
}

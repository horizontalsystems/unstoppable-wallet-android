package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData

@Composable
fun ChartBars(
    modifier: Modifier,
    chartData: ChartData,
) {
    val color =
        if (chartData.disabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob
    var valueMin = chartData.minValue
    val valueMax = chartData.maxValue
    if (valueMin == valueMax) {
        valueMin *= 0.9f
    }
    var timestampMin = chartData.startTimestamp
    var timestampMax = chartData.endTimestamp
    if (timestampMin == timestampMax) {
        timestampMin = (timestampMin * 0.9).toLong()
        timestampMax = (timestampMax * 1.1).toLong()
    }

    GraphicBars(
        modifier,
        chartData.valuesByTimestamp(),
        timestampMin,
        timestampMax,
        valueMin,
        valueMax,
        color
    )
}

@Composable
fun GraphicBars(
    modifier: Modifier = Modifier,
    data: LinkedHashMap<Long, Float>,
    minKey: Long,
    maxKey: Long,
    minValue: Float,
    maxValue: Float,
    color: Color
) {
    Canvas(
        modifier = modifier,
        onDraw = {
            val barMinHeight = 2.dp.toPx()
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = (canvasWidth - barWidth) / (maxKey - minKey)
            val yRatio = (canvasHeight - barMinHeight) / (maxValue - minValue)

            scale(scaleX = 1f, scaleY = -1f) {
                for ((timestamp, value) in data) {
                    if (value < minValue) continue

                    val x = (timestamp - minKey) * xRatio + barWidth / 2
                    val y = ((value - minValue) * yRatio + barMinHeight)

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

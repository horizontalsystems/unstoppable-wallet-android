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
        color,
        null
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
    color: Color,
    selectedItemKey: Long?
) {
    val dotColor = ComposeAppTheme.colors.leah

    Canvas(
        modifier = modifier,
        onDraw = {
            val barMinHeight = 2.dp.toPx()
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = (canvasWidth - barWidth) / (maxKey - minKey)
            val yRatio = (canvasHeight - barMinHeight) / (maxValue - minValue)

            var dotPosition: Offset? = null

            scale(scaleX = 1f, scaleY = -1f) {
                for ((timestamp, value) in data) {
                    if (value < minValue) continue

                    val x = (timestamp - minKey) * xRatio + barWidth / 2
                    val y = ((value - minValue) * yRatio + barMinHeight)

                    if (selectedItemKey == timestamp) {
                        dotPosition = Offset(x, y)
                    }

                    drawLine(
                        start = Offset(x = x, y = 0f),
                        end = Offset(x = x, y = y),
                        color = color,
                        strokeWidth = barWidth
                    )
                }
                dotPosition?.let {
                    drawCircle(dotColor, 5.dp.toPx(), center = it)
                }
            }
        }
    )
}

@Composable
fun GraphicBarsWithNegative(
    modifier: Modifier = Modifier,
    data: LinkedHashMap<Long, Float>,
    minKey: Long,
    maxKey: Long,
    minValue: Float,
    maxValue: Float,
    color: Color,
    colorNegative: Color
) {
    Canvas(
        modifier = modifier,
        onDraw = {
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = (canvasWidth - barWidth) / (maxKey - minKey)
            val yRatio = canvasHeight / (maxValue - minValue)

            val zeroY = (0f - minValue) * yRatio

            scale(scaleX = 1f, scaleY = -1f) {
                for ((timestamp, value) in data) {
                    if (value < minValue) continue

                    val x = (timestamp - minKey) * xRatio + barWidth / 2
                    val y = (value - minValue) * yRatio

                    drawLine(
                        start = Offset(x = x, y = zeroY),
                        end = Offset(x = x, y = y),
                        color = if (y > zeroY) color else colorNegative,
                        strokeWidth = barWidth
                    )
                }
            }
        }
    )
}

package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Indicator

@Composable
fun ChartBars(
    modifier: Modifier,
    chartData: ChartData,
) {
    val color = if (chartData.disabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob

    val valuesByTimestamp = chartData.valuesByTimestamp(Indicator.Candle)
    val valueMinRaw = valuesByTimestamp.values.minOrNull() ?: 0f
    val valueMaxRaw = valuesByTimestamp.values.maxOrNull() ?: 0f

    val valueMin by animateFloatAsState(targetValue = valueMinRaw, animationSpec = tween(1000))
    val valueMax by animateFloatAsState(targetValue = valueMaxRaw, animationSpec = tween(1000))

    val timestampMinRaw = chartData.startTimestamp.toInt()
    val timestampMin by animateIntAsState(
        targetValue = timestampMinRaw,
        animationSpec = tween(1000)
    )

    val timestampMaxRaw = chartData.endTimestamp.toInt()
    val timestampMax by animateIntAsState(
        targetValue = timestampMaxRaw,
        animationSpec = tween(1000)
    )

    Canvas(
        modifier = modifier,
        onDraw = {
            val barMinHeight = 2.dp.toPx()
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = (canvasWidth - barWidth) / (timestampMax - timestampMin)
            val yRatio = (canvasHeight - barMinHeight) / (valueMax - valueMin)

            scale(scaleX = 1f, scaleY = -1f) {
                valuesByTimestamp.forEach { (timestamp, valueRaw) ->
                    val value = valueRaw.coerceIn(valueMin, valueMax)

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

package cash.p.terminal.ui.compose.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.Indicator

@Composable
fun ChartBars(
    modifier: Modifier,
    chartData: ChartData,
) {
    val color = if (chartData.disabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.jacob

    val valuesByTimestamp = chartData.valuesByTimestamp(Indicator.Candle)

    val transitionState = remember { MutableTransitionState(valuesByTimestamp) }
    transitionState.targetState = valuesByTimestamp
    val transition = updateTransition(transitionState, label = "transition")

    val valueMin by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = ""
    ) {
        it.values.minOrNull() ?: 0f
    }
    val valueMax by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = ""
    ) {
        it.values.maxOrNull() ?: 0f
    }

    val timestampMin by transition.animateInt(
        transitionSpec = { tween(1000) },
        label = ""
    ) {
        it.keys.minOrNull()?.toInt() ?: 0
    }
    val timestampMax by transition.animateInt(
        transitionSpec = { tween(1000) },
        label = ""
    ) {
        it.keys.maxOrNull()?.toInt() ?: 0
    }

    Canvas(
        modifier = modifier,
        onDraw = {
            val currentState = transitionState.currentState
            val targetState = transitionState.targetState

            val from = currentState.keys.min().toInt()
            val to = targetState.keys.min().toInt()
            val current = timestampMin

            val valuesByTimestamp2 = xxx(currentState, targetState, fraction(from, to, current))

            val barMinHeight = 2.dp.toPx()
            val barWidth = 2.dp.toPx()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = (canvasWidth - barWidth) / (timestampMax - timestampMin)
            val yRatio = (canvasHeight - barMinHeight) / (valueMax - valueMin)

            scale(scaleX = 1f, scaleY = -1f) {
                for ((timestamp, valueRaw) in valuesByTimestamp2) {
                    val value = valueRaw.coerceAtMost(valueMax)

                    val x = (timestamp - timestampMin) * xRatio + barWidth / 2
                    val y = ((value - valueMin) * yRatio + barMinHeight)

                    if (y < 0) continue

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

private fun fraction(from: Int, to: Int, current: Int): Float {
    val diff = to - from
    val currentDiff = current - from

    return when {
        diff == 0 -> 1f
        currentDiff == 0 -> 0f
        else -> currentDiff / diff.toFloat()
    }
}

fun xxx(
    currentState: LinkedHashMap<Long, Float>,
    targetState: LinkedHashMap<Long, Float>,
    fraction: Float
) = when (fraction) {
    0f -> currentState
    1f -> targetState
    else -> {
        val keys = (currentState.keys + targetState.keys).distinct().sorted()
        val minValue = targetState.values.min()

        LinkedHashMap(
            keys.map {
                val fromValue = currentState[it] ?: (minValue / 100)
                val toValue = targetState[it] ?: (minValue / 100)
                val diff = toValue - fromValue
                val currentValue = fromValue + diff * fraction

                it to currentValue
            }.toMap()
        )
    }
}

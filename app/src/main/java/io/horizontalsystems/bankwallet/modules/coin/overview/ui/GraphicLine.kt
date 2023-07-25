package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun GraphicLine(
    modifier: Modifier,
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
            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = canvasWidth / (maxKey - minKey)
            val yRatio = canvasHeight / (maxValue - minValue)

            val linePath = Path()
            var pathStarted = false
            data.forEach { (key, value) ->
                val x = (key - minKey) * xRatio
                val y = (value - minValue) * yRatio

                if (!pathStarted) {
                    linePath.moveTo(x, y)
                    pathStarted = true
                } else {
                    linePath.lineTo(x, y)
                }
            }

            scale(scaleX = 1f, scaleY = -1f) {
                drawPath(
                    linePath,
                    color,
                    style = Stroke(1.dp.toPx())
                )
            }
        }
    )
}

@Composable
fun GraphicLineWithGradient(
    valuesByTimestamp: LinkedHashMap<Long, Float>,
    minKey: Long,
    maxKey: Long,
    minValue: Float,
    maxValue: Float,
    color: Color,
    gradientColors: Pair<Color, Color>,
    selectedItemKey: Long?
) {
    val dotColor = ComposeAppTheme.colors.leah
    Canvas(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth(),
        onDraw = {
            var dotPosition: Offset? = null

            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = canvasWidth / (maxKey - minKey)
            val yRatio = canvasHeight / (maxValue - minValue)

            val linePath = Path()
            var pathStarted = false
            valuesByTimestamp.forEach { (timestamp, value) ->
                val x = (timestamp - minKey) * xRatio
                val y = (value - minValue) * yRatio

                if (!pathStarted) {
                    linePath.moveTo(x, y)
                    pathStarted = true
                } else {
                    linePath.lineTo(x, y)
                }

                if (selectedItemKey == timestamp) {
                    dotPosition = Offset(x, y)
                }
            }

            val gradientPath = Path()
            gradientPath.addPath(linePath)

            gradientPath.lineTo(canvasWidth, 0f)
            gradientPath.lineTo(0f, 0f)
            gradientPath.close()

            scale(scaleX = 1f, scaleY = -1f) {
                drawPath(
                    linePath,
                    color,
                    style = Stroke(1.dp.toPx())
                )
                drawPath(
                    gradientPath,
                    Brush.verticalGradient(
                        0.00f to gradientColors.first,
                        1.00f to gradientColors.second,
                        tileMode = TileMode.Repeated
                    ),
                )
                dotPosition?.let {
                    drawCircle(dotColor, 5.dp.toPx(), center = it)
                }
            }
        }
    )
}
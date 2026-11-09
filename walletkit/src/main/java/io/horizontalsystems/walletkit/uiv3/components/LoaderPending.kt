package io.horizontalsystems.walletkit.uiv3.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

@Composable
fun LoaderPending(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    strokeWidth: Dp = 2.dp,
) {
    val trackColor = ComposeAppTheme.colors.andy
    val arcColor = ComposeAppTheme.colors.orange

    val transition = rememberInfiniteTransition(label = "LoaderPending")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "LoaderPendingRotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx())
        val halfStroke = stroke.width / 2
        val radius = this.size.minDimension / 2 - halfStroke

        drawCircle(color = trackColor, radius = radius, style = Stroke(width = stroke.width))

        rotate(rotation) {
            drawArc(
                brush = Brush.linearGradient(
                    0.2026f to arcColor,
                    0.5952f to Color.Transparent,
                    start = Offset(halfStroke, this.size.height - halfStroke),
                    end = Offset(this.size.width - halfStroke, halfStroke),
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(halfStroke, halfStroke),
                size = Size(radius * 2, radius * 2),
                style = stroke
            )
        }
    }
}

@Preview
@Composable
fun Preview_LoaderPending() {
    ComposeAppTheme(darkTheme = true) {
        LoaderPending()
    }
}

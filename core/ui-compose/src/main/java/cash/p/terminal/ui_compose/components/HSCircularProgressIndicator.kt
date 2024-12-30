package cash.p.terminal.ui_compose.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun HSCircularProgressIndicator(modifier: Modifier = Modifier) {
    // Todo: Need to find better solution
    // CircularProgressIndicator doesn't allow to change its size.
    // Resized it using modifier size with padding.
    // The ordering of modifiers is important.
    CircularProgressIndicator(
        modifier = modifier
            .size(28.dp)
            .padding(top = 4.dp, end = 8.dp),
        color = ComposeAppTheme.colors.grey,
        strokeWidth = 2.dp
    )
}

@Composable
fun HSCircularProgressIndicator(progress: Float) {
    val transition = rememberInfiniteTransition()
    val rotate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        )
    )
    CircularProgressIndicator(
        progress = progress,
        modifier = Modifier.rotate(rotate),
        color = ComposeAppTheme.colors.grey50,
        strokeWidth = 2.dp
    )
}

@Composable
fun HSCircularProgressIndicator(progress: Float, size: Dp) {
    val transition = rememberInfiniteTransition()
    val rotate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        )
    )
    CircularProgressIndicator(
        progress = progress,
        modifier = Modifier
            .size(size)
            .rotate(rotate),
        color = ComposeAppTheme.colors.grey50,
        strokeWidth = 2.dp
    )
}

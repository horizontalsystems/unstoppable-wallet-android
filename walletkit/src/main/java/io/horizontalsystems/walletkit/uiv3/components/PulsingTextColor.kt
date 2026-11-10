package io.horizontalsystems.walletkit.uiv3.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.designkit.theme.ComposeAppTheme

/**
 * Text colour that pulses between andy and grey, marking a status that is still in progress.
 */
@Composable
fun pulsingTextColor(): Color {
    val transition = rememberInfiniteTransition(label = "PulsingTextColor")
    val color by transition.animateColor(
        initialValue = ComposeAppTheme.colors.andy,
        targetValue = ComposeAppTheme.colors.grey,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 801, easing = EaseOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulsingTextColor"
    )
    return color
}

@Preview
@Composable
fun Preview_PulsingTextColor() {
    ComposeAppTheme(darkTheme = true) {
        Column {
            Text(
                text = "Receiving…",
                style = ComposeAppTheme.typography.subheadSB,
                color = pulsingTextColor(),
            )
        }
    }
}

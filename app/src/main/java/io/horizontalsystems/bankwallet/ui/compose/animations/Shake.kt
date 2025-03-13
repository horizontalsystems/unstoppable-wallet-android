package io.horizontalsystems.bankwallet.ui.compose.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.debugInspectorInfo

fun Modifier.shake(
    enabled: Boolean,
    onAnimationFinish: () -> Unit,
) = composed(
    factory = {
        val distance by animateFloatAsState(
            targetValue = if (enabled) 15f else 0f,
            animationSpec =
                repeatable(
                    iterations = 7,
                    animation = tween(durationMillis = 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            finishedListener = { onAnimationFinish.invoke() },
        )

        Modifier.graphicsLayer {
            translationX = if (enabled) distance else 0f
        }
    },
    inspectorInfo =
        debugInspectorInfo {
            name = "shake"
            properties["enabled"] = enabled
        },
)

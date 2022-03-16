package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

const val MIN_DRAG_AMOUNT = 3
fun Float.densityPixel(): Float = this * Resources.getSystem().displayMetrics.density

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun DraggableCardSimple(
    isRevealed: Boolean,
    cardOffset: Float,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    content: @Composable () -> Unit,
) {
    val transitionState = remember {
        MutableTransitionState(isRevealed).apply {
            targetState = !isRevealed
        }
    }
    val transition = updateTransition(transitionState, "cardTransition")

    val offsetTransition by transition.animateFloat(
        label = "cardOffsetTransition",
        transitionSpec = { tween(durationMillis = 200) },
        targetValueByState = { if (isRevealed) -cardOffset.densityPixel() else 0f },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetTransition.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    when {
                        dragAmount <= -MIN_DRAG_AMOUNT -> onExpand()
                        dragAmount > MIN_DRAG_AMOUNT -> onCollapse()
                    }
                }
            },
    ) {
        content()
    }
}

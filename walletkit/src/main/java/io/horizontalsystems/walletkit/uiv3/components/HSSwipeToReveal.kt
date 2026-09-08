package io.horizontalsystems.walletkit.uiv3.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class SwipeRevealValue {
    Concealed,
    Revealed,
    FullSwipe,
}

/**
 * Row container whose [content] can be dragged to the left to uncover [actions] underneath.
 *
 * The card follows the finger, settles on the nearest anchor with a spring, resists dragging
 * past the actions with a rubber band, and closes when the content is tapped while open.
 * The revealed width is measured from [actions], so callers do not pass an offset.
 *
 * [revealed] is the caller's source of truth (typically "this row is the one open in the
 * list"). Gesture-driven changes are reported through [onReveal] and [onConceal]; changing
 * [revealed] from outside animates the card accordingly.
 *
 * When [onFullSwipe] is set, dragging the card most of the way across the row commits the
 * action without tapping it, like a full swipe on iOS, and the card then animates back closed.
 */
@Composable
fun HSSwipeToReveal(
    revealed: Boolean,
    onReveal: () -> Unit,
    onConceal: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFullSwipe: (() -> Unit)? = null,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = remember {
        AnchoredDraggableState(
            initialValue = if (revealed) SwipeRevealValue.Revealed else SwipeRevealValue.Concealed
        )
    }
    val anchorsHolder = remember { AnchorsHolder(state) }
    val rubberBand = remember { RubberBandOverscrollEffect { anchorsHolder.containerWidth.toFloat() } }
    val coroutineScope = rememberCoroutineScope()
    val fullSwipeEnabled = onFullSwipe != null

    val currentRevealed by rememberUpdatedState(revealed)
    val currentOnReveal by rememberUpdatedState(onReveal)
    val currentOnConceal by rememberUpdatedState(onConceal)
    val currentOnFullSwipe by rememberUpdatedState(onFullSwipe)

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { settled ->
            when (settled) {
                SwipeRevealValue.Concealed -> if (currentRevealed) currentOnConceal()
                SwipeRevealValue.Revealed -> if (!currentRevealed) currentOnReveal()
                SwipeRevealValue.FullSwipe -> {
                    currentOnFullSwipe?.invoke()
                    state.animateTo(SwipeRevealValue.Concealed)
                }
            }
        }
    }

    val target = if (revealed) SwipeRevealValue.Revealed else SwipeRevealValue.Concealed
    LaunchedEffect(target, anchorsHolder.hasRevealAnchor) {
        if (state.anchors.hasPositionFor(target) && state.targetValue != target) {
            state.animateTo(target)
        }
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    Layout(
        content = {
            Box(propagateMinConstraints = true) { actions() }
            Box {
                content()
                if (state.settledValue != SwipeRevealValue.Concealed) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    coroutineScope.launch {
                                        state.animateTo(SwipeRevealValue.Concealed)
                                    }
                                }
                            }
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                overscrollEffect = rubberBand,
                flingBehavior = flingBehavior,
            ),
    ) { measurables, constraints ->
        val actionsMeasurable = measurables[0]
        val contentMeasurable = measurables[1]

        val contentPlaceable = contentMeasurable.measure(constraints)
        val width = contentPlaceable.width
        val height = contentPlaceable.height

        val naturalActionsWidth = actionsMeasurable.maxIntrinsicWidth(height).coerceAtMost(width)
        anchorsHolder.update(naturalActionsWidth, width, fullSwipeEnabled)

        val stateOffset = state.offset.takeUnless { it.isNaN() } ?: 0f
        val visualOffset = (stateOffset + rubberBand.offset).roundToInt()

        // Past the revealed anchor the actions stretch to follow the card, so no gap opens
        // between the card edge and the buttons during a full swipe or a rubber-band pull.
        val actionsWidth = max(naturalActionsWidth, -visualOffset).coerceAtMost(width)
        val actionsPlaceable = actionsMeasurable.measure(
            Constraints(minWidth = actionsWidth, maxWidth = actionsWidth, minHeight = height, maxHeight = height)
        )

        layout(width, height) {
            actionsPlaceable.place(width - actionsPlaceable.width, 0)
            contentPlaceable.place(visualOffset, 0)
        }
    }
}

/**
 * Closes the open swipe row when the list starts scrolling, so a revealed action never
 * scrolls away with the list.
 */
@Composable
fun ConcealOnScroll(listState: LazyListState, onConceal: () -> Unit) {
    val currentOnConceal by rememberUpdatedState(onConceal)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { currentOnConceal() }
    }
}

private class AnchorsHolder(private val state: AnchoredDraggableState<SwipeRevealValue>) {
    var actionsWidth = 0
        private set
    var containerWidth = 0
        private set
    private var fullSwipe = false

    var hasRevealAnchor by mutableStateOf(false)
        private set

    fun update(actionsWidth: Int, containerWidth: Int, fullSwipe: Boolean) {
        if (this.actionsWidth == actionsWidth && this.containerWidth == containerWidth && this.fullSwipe == fullSwipe) return
        this.actionsWidth = actionsWidth
        this.containerWidth = containerWidth
        this.fullSwipe = fullSwipe

        state.updateAnchors(
            DraggableAnchors {
                SwipeRevealValue.Concealed at 0f
                if (actionsWidth > 0) {
                    SwipeRevealValue.Revealed at -actionsWidth.toFloat()
                }
                if (fullSwipe && containerWidth > actionsWidth) {
                    SwipeRevealValue.FullSwipe at -containerWidth.toFloat()
                }
            }
        )
        hasRevealAnchor = actionsWidth > 0
    }
}

/**
 * Lets the card keep moving past its last anchor with increasing resistance, then springs it
 * back on release. Only the swipe direction (leftwards) resists; a rightward pull on a closed
 * card does nothing, as on iOS rows that have no leading actions.
 */
private class RubberBandOverscrollEffect(
    private val limit: () -> Float,
) : OverscrollEffect {

    private var raw by mutableFloatStateOf(0f)

    val offset: Float
        get() {
            val d = limit().takeIf { it > 0f } ?: return 0f
            val x = abs(raw)
            return -(1f - 1f / (x * RESISTANCE / d + 1f)) * d
        }

    override val isInProgress: Boolean
        get() = raw != 0f

    override val node: DelegatableNode = object : Modifier.Node() {}

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        var dx = delta.x
        if (raw < 0f && dx > 0f) {
            val unwind = if (dx >= -raw) -raw else dx
            raw += unwind
            dx -= unwind
        }
        val consumed = performScroll(Offset(dx, 0f))
        val leftover = dx - consumed.x
        if (source == NestedScrollSource.UserInput && leftover < 0f) {
            raw += leftover
        }
        return delta
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        performFling(velocity)
        if (raw != 0f) {
            animate(raw, 0f, animationSpec = spring(stiffness = Spring.StiffnessMedium)) { value, _ ->
                raw = value
            }
        }
    }

    companion object {
        private const val RESISTANCE = 0.55f
    }
}

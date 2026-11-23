package io.horizontalsystems.walletkit.modules.coin.overview.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import io.horizontalsystems.chartview.ChartTransition

/**
 * How every chart built on [PriceVolChart] animates when it appears and when its data changes. Change it here.
 */
object ChartAnimation {
    /** The path from the old data to the new: one of the [ChartTransition] types, or your own. */
    val transition: ChartTransition = ChartTransition.Morph

    /** How a chart comes in when it is first shown and has no old data to leave. */
    val appear: ChartTransition = ChartTransition.Rise

    /** The timing: duration and easing, or a `spring(...)` for a bounce. */
    val spec: AnimationSpec<Float> = tween(durationMillis = 700, easing = FastOutSlowInEasing)
}

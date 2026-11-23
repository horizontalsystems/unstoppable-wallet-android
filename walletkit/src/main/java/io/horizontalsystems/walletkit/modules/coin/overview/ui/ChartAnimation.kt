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

    // The fades are done at this fraction of the animation, about 55% of its time with the
    // easing below, while a data change in the same animation still takes all of it.
    private const val FADE_SHARE = 0.83f

    /** How a layer comes in on a chart that is already shown, such as an indicator being switched on. */
    val show: ChartTransition = ChartTransition.FadeIn(completeAt = FADE_SHARE)

    /** How a layer goes out when its data is taken away, such as an indicator being switched off. */
    val hide: ChartTransition = ChartTransition.FadeOut(completeAt = FADE_SHARE)

    /** The timing: duration and easing, or a `spring(...)` for a bounce. */
    val spec: AnimationSpec<Float> = tween(durationMillis = 420, easing = FastOutSlowInEasing)
}

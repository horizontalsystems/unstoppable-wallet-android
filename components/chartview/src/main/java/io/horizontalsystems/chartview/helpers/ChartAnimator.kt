package io.horizontalsystems.chartview.helpers

import android.animation.ValueAnimator
import android.view.animation.AccelerateInterpolator

class ChartAnimator(onUpdate: (() -> Unit)) {

    private var animatedFraction = 0f
    private var animator = ValueAnimator()

    init {
        animator.interpolator = AccelerateInterpolator()
        animator.duration = 300
        animator.addUpdateListener {
            animatedFraction = animator.animatedFraction
            onUpdate()
        }
    }

    fun start() {
        animator.setFloatValues(0f)
        animator.start()
    }

    fun getAnimatedY(y: Float, maxY: Float): Float {
        // Figure out top of column based on INVERSE of percentage. Bigger the percentage,
        // the smaller top is, since 100% goes to 0.
        return maxY - (maxY - y) * animatedFraction
    }
}
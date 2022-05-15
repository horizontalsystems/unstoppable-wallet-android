package io.horizontalsystems.chartview.helpers

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class ChartAnimator(onUpdate: (() -> Unit)) {

    var animatedFraction = 0f
        private set
    private var animator = ValueAnimator()

    init {
        animator.interpolator = LinearInterpolator()
        animator.duration = 300
        animator.addUpdateListener {
            animatedFraction = animator.animatedFraction
            onUpdate()
        }
    }

    fun addUpdateListener(listener: ValueAnimator.AnimatorUpdateListener) {
        animator.addUpdateListener(listener)
    }

    fun start() {
        animator.setFloatValues(0f)
        animator.start()
    }

    fun cancel() {
        animator.cancel()
    }

    fun getAnimatedY(y: Float, maxY: Float): Float {
        // Figure out top of column based on INVERSE of percentage. Bigger the percentage,
        // the smaller top is, since 100% goes to 0.
        return maxY - (maxY - y) * animatedFraction
    }
}
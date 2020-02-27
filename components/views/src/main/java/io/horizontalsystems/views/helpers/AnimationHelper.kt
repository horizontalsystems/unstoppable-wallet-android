package io.horizontalsystems.views.helpers

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

object AnimationHelper {

    fun expand(v: View) {
        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val targetHeight = v.measuredHeight

        v.alpha = 0.3f
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.visibility = View.VISIBLE
                v.layoutParams.height = if (interpolatedTime == 1f) targetHeight else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
                if (interpolatedTime > 0.3f) {
                    v.alpha = interpolatedTime
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // 1dp/ms
        a.duration = (((targetHeight / v.context.resources.displayMetrics.density)) * 2).toLong()
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredHeight

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                    if (interpolatedTime > 0.3f) {
                        v.alpha = (1 - interpolatedTime) * 2
                    }
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // 1dp/ms
        a.duration = (((initialHeight / v.context.resources.displayMetrics.density)) * 2).toLong()

        a.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {}

            override fun onAnimationStart(animation: Animation?) {
                v.postDelayed({
                    if (v.visibility != View.GONE) {
                        v.visibility = View.GONE
                    }
                }, a.duration + 100)
            }
        })

        v.startAnimation(a)
    }
}

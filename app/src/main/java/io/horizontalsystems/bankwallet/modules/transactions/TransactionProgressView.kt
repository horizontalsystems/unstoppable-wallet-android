package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_progress.view.*


class TransactionProgressView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        inflate(context, R.layout.view_transaction_progress, this)
    }

    private var threadHandler: Handler? = null
    private var runnable: Runnable? = null
    private var animationDelayTime: Long = 150
    private var lastAnimatedIndex = 0
    private var progressIndex = 0

    fun bind(progress: Double) {
        when {
            progress <= 0.33 -> {
                progressIndex = 0
            }
            progress <= 0.66 -> {
                progressIndex = 1
                animateFirstBar()
            }
            progress <= 1.0 -> {
                progressIndex = 2
                animateFirstBar()
                animateSecondBar()
            }
            else -> {
                animateFirstBar()
                animateSecondBar()
                animateThirdBar()
                return
            }
        }

        animateProgress()
    }

    private fun animateProgress() {
        if (threadHandler == null && runnable == null) {
            lastAnimatedIndex = progressIndex

            threadHandler = Handler()
            runnable = object : Runnable {
                override fun run() {

                    threadHandler?.postDelayed(this, animationDelayTime)
                    if (lastAnimatedIndex == 0) {
                        animateFirstBar()
                        lastAnimatedIndex = 1
                    } else if (lastAnimatedIndex == 1) {
                        animateSecondBar()
                        lastAnimatedIndex = 2
                    } else if (lastAnimatedIndex == 2) {
                        animateThirdBar()
                        lastAnimatedIndex = 3
                    } else {
                        resetBarsColor()
                        lastAnimatedIndex = progressIndex
                    }
                    invalidate()
                }
            }
            runnable?.run()
        }
    }

    private fun resetBarsColor() {
        if (progressIndex < 1) {
            progressBar1.setBackgroundResource(R.drawable.status_progress_bar_grey_20_small)
        }
        if (progressIndex < 2) {
            progressBar2.setBackgroundResource(R.drawable.status_progress_bar_grey_20_small)
        }
        if (progressIndex < 3) {
            progressBar3.setBackgroundResource(R.drawable.status_progress_bar_grey_20_small)
        }
    }

    private fun animateFirstBar() {
        progressBar1.setBackgroundResource(R.drawable.bar_frame_animated)
        val frameAnimation = progressBar1.background as AnimationDrawable
        frameAnimation.start()
    }

    private fun animateSecondBar() {
        progressBar2.setBackgroundResource(R.drawable.bar_frame_animated)
        val frameAnimation2 = progressBar2.background as AnimationDrawable
        frameAnimation2.start()
    }

    private fun animateThirdBar() {
        progressBar3.setBackgroundResource(R.drawable.bar_frame_animated)
        val frameAnimation3 = progressBar3.background as AnimationDrawable
        frameAnimation3.start()
    }

}

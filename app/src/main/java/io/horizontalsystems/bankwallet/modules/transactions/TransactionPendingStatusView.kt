package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_pending_status.view.*


class TransactionPendingStatusView : ConstraintLayout {

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
        inflate(context, R.layout.view_transaction_pending_status, this)
    }

    private var threadHandler: Handler? = null
    private var runnable: Runnable? = null
    private var animationDelayTime: Long = 150

    fun startAnimation() {
        if (threadHandler == null && runnable == null) {
            var lastAnimatedIndex = 1
            threadHandler = Handler()
            runnable = object : Runnable {
                override fun run() {

                    threadHandler?.postDelayed(this, animationDelayTime)
                    if (lastAnimatedIndex == 1) {
                        pendingText.text = context.getString(R.string.Transactions_PendingOneDot)
                        lastAnimatedIndex = 2
                    } else if (lastAnimatedIndex == 2) {
                        pendingText.text = context.getString(R.string.Transactions_PendingTwoDots)
                        lastAnimatedIndex = 3
                    } else if (lastAnimatedIndex == 3) {
                        pendingText.text = context.getString(R.string.Transactions_PendingThreeDots)
                        lastAnimatedIndex = 4
                    } else {
                        pendingText.text = context.getString(R.string.Transactions_Pending)
                        lastAnimatedIndex = 1
                    }
                    invalidate()
                }
            }
            runnable?.run()
        }
    }

}

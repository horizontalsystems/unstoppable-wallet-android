package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_status.view.*


open class TransactionStatusWithTimeView : ConstraintLayout {

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
        inflate(context, R.layout.view_transaction_status, this)
    }

    fun bind(transactionStatus: TransactionStatus, time: String?) {
        txTime.text = time
        txTime.visibility = View.VISIBLE
        progressBarWrapper.visibility = View.GONE
        pendingIcon.visibility = View.GONE
        completedIcon.visibility = View.GONE

        when (transactionStatus) {
            is TransactionStatus.Completed -> {
                completedIcon.visibility = View.VISIBLE
            }
            is TransactionStatus.Processing -> {
                progressBarWrapper.visibility = View.VISIBLE
                fillProgress(transactionStatus)
            }
            else -> {
                txTime.visibility = View.GONE
                pendingIcon.visibility = View.VISIBLE
                progressBarWrapper.visibility = View.VISIBLE
                fillProgress()
            }
        }
        invalidate()
    }

    private fun fillProgress(transactionStatus: TransactionStatus.Processing = TransactionStatus.Processing(0.0)) {
        val bars = listOf(progressBar1, progressBar2, progressBar3)
        val filledBars = bars.size * transactionStatus.progress

        bars.forEachIndexed { index, bar ->
            bar.setImageResource(if (filledBars > 0.0 && index < filledBars)
                R.drawable.status_progress_bar_grey_small else
                R.drawable.status_progress_bar_grey_20_small)
        }
    }
}

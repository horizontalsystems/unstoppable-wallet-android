package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
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
        ConstraintLayout.inflate(context, R.layout.view_transaction_status, this)
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

    private fun fillProgress(transactionStatus: TransactionStatus.Processing = TransactionStatus.Processing(0)) {
        val grey20Bar = R.drawable.status_progress_bar_grey_20_small
        val solidGreyBar = R.drawable.status_progress_bar_grey_small

        progressBar1.setImageResource(if (transactionStatus.progress >= 1) solidGreyBar else grey20Bar)
        progressBar2.setImageResource(if (transactionStatus.progress >= 2) solidGreyBar else grey20Bar)
        progressBar3.setImageResource(if (transactionStatus.progress >= 3) solidGreyBar else grey20Bar)
        progressBar4.setImageResource(if (transactionStatus.progress >= 4) solidGreyBar else grey20Bar)
        progressBar5.setImageResource(if (transactionStatus.progress >= 5) solidGreyBar else grey20Bar)
        progressBar6.setImageResource(if (transactionStatus.progress >= 6) solidGreyBar else grey20Bar)
    }

}

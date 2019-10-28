package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.view_transaction_info_status.view.*


open class TransactionInfoStatusView : ConstraintLayout {

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
        inflate(context, R.layout.view_transaction_info_status, this)
    }

    fun bind(transactionStatus: TransactionStatus) {
        progressBarWrapper.visibility = View.GONE
        statusIcon.visibility = View.GONE
        statusPendingText.visibility = View.GONE

        when (transactionStatus) {
            is TransactionStatus.Completed -> {
                statusIcon.setImageDrawable(LayoutHelper.d(R.drawable.ic_checkmark_green, App.instance))
                statusIcon.visibility = View.VISIBLE
                statusPendingText.visibility = View.VISIBLE
                statusPendingText.text = context.getString(R.string.TransactionInfo_Status_Confirmed)
            }
            is TransactionStatus.Processing -> {
                progressBarWrapper.visibility = View.VISIBLE
                fillProgress(transactionStatus)
            }
            else -> {
                statusIcon.setImageDrawable(LayoutHelper.d(R.drawable.ic_pending_grey, App.instance))
                statusIcon.visibility = View.VISIBLE
                statusPendingText.visibility = View.VISIBLE
                statusPendingText.text = context.getString(R.string.TransactionInfo_Status_Pending)
            }
        }
        invalidate()
    }

    private fun fillProgress(transactionStatus: TransactionStatus.Processing) {
        val progress = transactionStatus.progress
        progressBar1.setImageResource(if(progress >= 0.33) R.drawable.status_progress_bar_green else R.drawable.status_progress_bar_grey)
        progressBar2.setImageResource(if(progress >= 0.66) R.drawable.status_progress_bar_green else R.drawable.status_progress_bar_grey)
        progressBar3.setImageResource(if(progress > 1.0) R.drawable.status_progress_bar_green else R.drawable.status_progress_bar_grey)
    }

}

package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
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

    fun bind(transactionStatus: TransactionStatus, incoming: Boolean) {
        progressBarWrapper.visibility = View.GONE
        confirmedText.visibility = View.GONE
        progressText.visibility = View.GONE
        failedText.visibility = View.GONE

        when (transactionStatus) {
            is TransactionStatus.Failed -> {
                failedText.visibility = View.VISIBLE
            }
            is TransactionStatus.Completed -> {
                confirmedText.visibility = View.VISIBLE
            }
            is TransactionStatus.Processing -> {
                fillProgress(transactionStatus.progress, incoming)
            }
            else -> {
                fillProgress(incoming = incoming)
            }
        }
        invalidate()
    }

    private fun fillProgress(progress: Double = 0.0, incoming: Boolean) {
        progressBar1.setImageResource(if (progress >= 0.33) getColoredBar(incoming) else R.drawable.status_progress_bar_grey)
        progressBar2.setImageResource(if (progress >= 0.66) getColoredBar(incoming) else R.drawable.status_progress_bar_grey)
        progressBar3.setImageResource(if (progress > 1.0) getColoredBar(incoming) else R.drawable.status_progress_bar_grey)
        progressBarWrapper.visibility = View.VISIBLE

        progressText.setText(if (incoming) R.string.Transactions_Receiving else R.string.Transactions_Sending)
        progressText.visibility = View.VISIBLE
    }

    private fun getColoredBar(incoming: Boolean) = if (incoming) R.drawable.status_progress_bar_green else R.drawable.status_progress_bar_yellow

}

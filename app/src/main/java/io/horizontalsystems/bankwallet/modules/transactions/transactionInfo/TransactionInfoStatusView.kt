package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import kotlinx.android.synthetic.main.view_transaction_info_status.view.*
import kotlinx.android.synthetic.main.view_transaction_info_status.view.progressBar


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
        progressBar.isVisible = false
        confirmedText.isVisible = false
        progressText.isVisible = false
        failedText.isVisible = false

        when (transactionStatus) {
            is TransactionStatus.Failed -> {
                failedText.isVisible = true
            }
            is TransactionStatus.Pending -> {
                fillProgress(incoming = incoming)
                setText(R.string.Transactions_Pending)
            }
            is TransactionStatus.Completed -> {
                confirmedText.isVisible = true
            }
            is TransactionStatus.Processing -> {
                fillProgress(transactionStatus.progress, incoming)
                setText(if (incoming) R.string.Transactions_Receiving else R.string.Transactions_Sending)
            }
        }
        invalidate()
    }

    private fun setText(textRes: Int) {
        progressText.setText(textRes)
        progressText.isVisible = true
    }

    private fun fillProgress(progress: Double = 0.0, incoming: Boolean) {
        when {
            progress <= 0.33 -> progressBar.setBackgroundResource(R.drawable.animation_pending)
            progress <= 0.66 -> progressBar.setBackgroundResource(if (incoming) R.drawable.animation_receiving_progress_30 else R.drawable.animation_sending_progress_30)
            progress <= 1.0 -> progressBar.setBackgroundResource(if (incoming) R.drawable.animation_receiving_progress_60 else R.drawable.animation_sending_progress_60)
        }

        progressBar.isVisible = true
        (progressBar.background as? AnimationDrawable)?.start()
    }

}

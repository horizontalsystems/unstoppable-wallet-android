package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus

open class TransactionInfoStatusView : ConstraintLayout {

    private lateinit var progressBar: ProgressBar
    private lateinit var confirmedText: TextView
    private lateinit var progressText: TextView
    private lateinit var failedText: TextView

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
        val rootView = inflate(context, R.layout.view_transaction_info_status, this)
        progressBar = rootView.findViewById(R.id.progressBar)
        confirmedText = rootView.findViewById(R.id.confirmedText)
        progressText = rootView.findViewById(R.id.progressText)
        failedText = rootView.findViewById(R.id.failedText)
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

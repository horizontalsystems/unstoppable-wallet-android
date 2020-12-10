package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionType
import kotlinx.android.synthetic.main.view_transaction_status.view.*


class TransactionStatusWithTimeView : ConstraintLayout {

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

    fun bind(transactionStatus: TransactionStatus, type: TransactionType, time: String?) {
        txTime.isVisible = false
        statusText.isVisible = false

        when (transactionStatus) {
            is TransactionStatus.Failed -> {
                statusText.isVisible = true
                statusText.setText(R.string.Transactions_Failed)
            }
            is TransactionStatus.Pending -> {
                statusText.isVisible = true
                statusText.setText(R.string.Transactions_Pending)
            }
            is TransactionStatus.Processing -> {
                statusText.isVisible = true
                statusText.setText(getText(type))
            }
            is TransactionStatus.Completed -> {
                txTime.text = time
                txTime.isVisible = true
            }
        }
        invalidate()
    }

    private fun getText(type: TransactionType) = when (type) {
        TransactionType.Outgoing, TransactionType.SentToSelf -> R.string.Transactions_Sending
        TransactionType.Incoming -> R.string.Transactions_Receiving
        TransactionType.Approve -> R.string.Transactions_Approving
    }

}

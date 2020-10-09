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
        completedIcon.isVisible = false
        transactionProgressView.isVisible = false
        failedText.isVisible = false

        when (transactionStatus) {
            is TransactionStatus.Failed -> {
                failedText.isVisible = true
            }
            is TransactionStatus.Completed -> {
                txTime.text = time
                txTime.isVisible = true
                completedIcon.isVisible = true
            }
            is TransactionStatus.Processing -> {
                transactionProgressView.bind(transactionStatus.progress, type)
                transactionProgressView.isVisible = true
            }
            is TransactionStatus.Pending -> {
                transactionProgressView.bind(type = type)
                transactionProgressView.isVisible = true
            }
        }
        invalidate()
    }

}

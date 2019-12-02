package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
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

    fun bind(transactionStatus: TransactionStatus, incoming: Boolean, time: String?) {
        txTime.visibility = View.GONE
        completedIcon.visibility = View.GONE
        transactionProgressView.visibility = View.GONE
        failedText.visibility = View.GONE

        when (transactionStatus) {
            is TransactionStatus.Failed -> {
                failedText.visibility = View.VISIBLE
            }
            is TransactionStatus.Completed -> {
                txTime.text = time
                txTime.visibility = View.VISIBLE
                completedIcon.visibility = View.VISIBLE
            }
            is TransactionStatus.Processing -> {
                transactionProgressView.bind(transactionStatus.progress, incoming)
                transactionProgressView.visibility = View.VISIBLE
            }
            else -> {
                transactionProgressView.bind(incoming = incoming)
                transactionProgressView.visibility = View.VISIBLE
            }
        }
        invalidate()
    }

}

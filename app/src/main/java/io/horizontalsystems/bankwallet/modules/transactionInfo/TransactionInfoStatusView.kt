package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*
import kotlinx.android.synthetic.main.view_transaction_info_status.view.*


class TransactionInfoStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_transaction_info_status, this)
    }

    fun bind(transactionStatus: TransactionStatusViewItem) {
        statusText.isVisible = false
        failedText.isVisible = false
        iconCheckmark.isVisible = false
        progressSpinner.isVisible = false

        when (transactionStatus) {
            is Failed -> {
                failedText.isVisible = true
            }
            is Pending -> {
                progressSpinner.setProgressColored(15, context.getColor(R.color.grey_50), true)
                progressSpinner.isVisible = true
                setText(transactionStatus.name)
            }
            is Completed -> {
                setText(transactionStatus.name)
                iconCheckmark.isVisible = true
            }
            is Processing -> {
                val progressValue = (transactionStatus.progress * 100).toInt()
                progressSpinner.setProgressColored(
                    progressValue,
                    context.getColor(R.color.grey_50),
                    true
                )
                progressSpinner.isVisible = true
                setText(transactionStatus.name)
            }
        }
        invalidate()
    }

    private fun setText(text: String) {
        statusText.text = text
        statusText.isVisible = true
    }

}

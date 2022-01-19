package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewTransactionInfoStatusBinding
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*

class TransactionInfoStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewTransactionInfoStatusBinding.inflate(LayoutInflater.from(context), this)

    fun bind(transactionStatus: TransactionStatusViewItem) {
        binding.statusText.isVisible = false
        binding.failedText.isVisible = false
        binding.iconCheckmark.isVisible = false
        binding.progressSpinner.isVisible = false

        when (transactionStatus) {
            is Failed -> {
                binding.failedText.isVisible = true
            }
            is Pending -> {
                binding.progressSpinner.setProgressColored(
                    15,
                    context.getColor(R.color.grey_50),
                    true
                )
                binding.progressSpinner.isVisible = true
                setText(transactionStatus.name)
            }
            is Completed -> {
                setText(transactionStatus.name)
                binding.iconCheckmark.isVisible = true
            }
            is Processing -> {
                val progressValue = (transactionStatus.progress * 100).toInt()
                binding.progressSpinner.setProgressColored(
                    progressValue,
                    context.getColor(R.color.grey_50),
                    true
                )
                binding.progressSpinner.isVisible = true
                setText(transactionStatus.name)
            }
        }
        invalidate()
    }

    private fun setText(text: String) {
        binding.statusText.text = text
        binding.statusText.isVisible = true
    }

}

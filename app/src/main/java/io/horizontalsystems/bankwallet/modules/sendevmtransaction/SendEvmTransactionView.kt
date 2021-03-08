package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel.ViewItem
import kotlinx.android.synthetic.main.view_send_evm_transaction.view.*

class SendEvmTransactionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_send_evm_transaction, this)
    }

    fun init(
            transactionViewModel: SendEvmTransactionViewModel,
            ethereumFeeViewModel: EthereumFeeViewModel,
            viewLifecycleOwner: LifecycleOwner,
            fragmentManager: FragmentManager,
            showSpeedInfoListener: () -> Unit
    ) {
        feeSelectorView.setFeeSelectorViewInteractions(
                ethereumFeeViewModel,
                ethereumFeeViewModel,
                viewLifecycleOwner,
                fragmentManager,
                showSpeedInfoListener
        )

        transactionViewModel.viewItems.forEach { viewItem ->
            when (viewItem) {
                is ViewItem.Amount -> amountValue.text = viewItem.value
                is ViewItem.Input -> inputValue.text = viewItem.value
                is ViewItem.To -> toAddressValue.text = viewItem.value
            }
        }

        transactionViewModel.errorLiveData.observe(viewLifecycleOwner, {
            error.text = it
        })
    }

}

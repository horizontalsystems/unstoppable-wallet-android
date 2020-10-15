package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_wallet_connect_request.*
import kotlinx.android.synthetic.main.partial_transaction_info.*

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {

    private val baseViewModel by activityViewModels<WalletConnectViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sentToSelfIcon.isVisible = false
        primaryValue.setTextColor(LayoutHelper.getAttr(R.attr.ColorJacob, requireContext().theme)
                ?: requireContext().getColor(R.color.yellow_d))

        val viewModel by viewModels<WalletConnectSendEthereumTransactionRequestViewModel> { WalletConnectRequestModule.Factory(baseViewModel.sharedSendEthereumTransactionRequest!!) }

        viewModel.resultLiveData.observe(viewLifecycleOwner, Observer {
//            dismiss()
        })

        viewModel.amountViewItemLiveData.observe(viewLifecycleOwner, Observer {
            it.primaryAmountInfo.let {
                primaryName.text = it.getAmountName()
                primaryValue.text = it.getFormattedForTxInfo()
            }

            it.secondaryAmountInfo.let {
                secondaryName.text = it?.getAmountName()
                secondaryValue.text = it?.getFormattedForTxInfo()
            }
        })

        btnApprove.setOnSingleClickListener {
            viewModel.approve()
        }

        btnReject.setOnSingleClickListener {
//            viewModel.reject()
        }
    }

    companion object {
        val keyRequest = "keyRequest"

        fun newInstance(): WalletConnectSendEthereumTransactionRequestFragment {
            return WalletConnectSendEthereumTransactionRequestFragment()
        }

    }

}
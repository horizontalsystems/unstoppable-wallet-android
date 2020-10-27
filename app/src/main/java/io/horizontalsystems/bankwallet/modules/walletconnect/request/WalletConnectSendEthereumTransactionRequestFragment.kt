package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_wallet_connect_request.*
import kotlinx.android.synthetic.main.partial_transaction_info.*

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sentToSelfIcon.isVisible = false
        primaryValue.setTextColor(LayoutHelper.getAttr(R.attr.ColorJacob, requireContext().theme)
                ?: requireContext().getColor(R.color.yellow_d))

        val viewModel by viewModels<WalletConnectSendEthereumTransactionRequestViewModel> { WalletConnectRequestModule.Factory(baseViewModel.sharedSendEthereumTransactionRequest!!) }

        btnApprove.setOnSingleClickListener {
            viewModel.approve()
        }

        btnReject.setOnSingleClickListener {
//            viewModel.reject()
        }

        viewModel.amountData.let {
            it.primary.let {
                primaryName.text = it.getAmountName()
                primaryValue.text = it.getFormattedForTxInfo()
            }

            it.secondary.let {
                secondaryName.text = it?.getAmountName()
                secondaryValue.text = it?.getFormattedForTxInfo()
            }
        }

        viewModel.viewItems

        viewModel.approveLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().popBackStack()
        })

        viewModel.approveEnabledLiveData.observe(viewLifecycleOwner, Observer {
            btnApprove.isEnabled = it
        })

        viewModel.rejectEnabledLiveData.observe(viewLifecycleOwner, Observer {
            btnReject.isEnabled = it
        })

    }

    companion object {
        val keyRequest = "keyRequest"

        fun newInstance(): WalletConnectSendEthereumTransactionRequestFragment {
            return WalletConnectSendEthereumTransactionRequestFragment()
        }

    }

}
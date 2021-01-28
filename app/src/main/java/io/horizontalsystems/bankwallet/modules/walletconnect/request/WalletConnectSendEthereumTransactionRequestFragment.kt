package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_wallet_connect_request.*
import kotlinx.android.synthetic.main.partial_transaction_info.*
import kotlinx.android.synthetic.main.view_transaction_info_item.*

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {

    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sentToSelfIcon.isVisible = false
        primaryValue.setTextColor(requireContext().getColor(R.color.jacob))

        val vmFactory = WalletConnectRequestModule.Factory(baseViewModel.sharedSendEthereumTransactionRequest!!)

        val viewModel by viewModels<WalletConnectSendEthereumTransactionRequestViewModel> { vmFactory }
        val feeViewModel by viewModels<EthereumFeeViewModel> { vmFactory }

        btnApprove.setOnSingleClickListener {
            viewModel.approve()
        }

        btnReject.setOnSingleClickListener {
            popBackStackWithResult(ApproveResult.Rejected)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            popBackStackWithResult(ApproveResult.Rejected)
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

        val detailsAdapter = TransactionDetailsAdapter(viewModel.viewItems)

        rvDetails.adapter = detailsAdapter

        viewModel.approveLiveEvent.observe(viewLifecycleOwner, Observer { transactionHash ->
            popBackStackWithResult(ApproveResult.Approved(transactionHash))
        })

        viewModel.approveEnabledLiveData.observe(viewLifecycleOwner, Observer {
            btnApprove.isEnabled = it
        })

        viewModel.rejectEnabledLiveData.observe(viewLifecycleOwner, Observer {
            btnReject.isEnabled = it
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            error.text = it
        })

        feeSelectorView.setFeeSelectorViewInteractions(feeViewModel, feeViewModel, viewLifecycleOwner, parentFragmentManager)
    }

    private fun popBackStackWithResult(approveResult: ApproveResult) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set("ApproveResult", approveResult)
        findNavController().popBackStack()
    }

    sealed class ApproveResult {
        @Parcelize
        class Approved(val txHash: ByteArray) : ApproveResult(), Parcelable

        @Parcelize
        object Rejected : ApproveResult(), Parcelable
    }
}

class TransactionDetailsAdapter(items: List<WalletConnectRequestViewItem>) : ListAdapter<WalletConnectRequestViewItem, WalletConnectRequestViewItemViewHolder>(diffCallback) {

    init {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletConnectRequestViewItemViewHolder {
        return WalletConnectRequestViewItemViewHolder(inflate(parent, R.layout.view_holder_transaction_info))
    }

    override fun onBindViewHolder(holder: WalletConnectRequestViewItemViewHolder, position: Int) {
        holder.bind(getItem(position), position < itemCount - 1)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<WalletConnectRequestViewItem>() {
            override fun areItemsTheSame(oldItem: WalletConnectRequestViewItem, newItem: WalletConnectRequestViewItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: WalletConnectRequestViewItem, newItem: WalletConnectRequestViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class WalletConnectRequestViewItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: WalletConnectRequestViewItem, borderVisible: Boolean) {
        btnAction.isVisible = false
        valueText.isVisible = false
        transactionStatusView.isVisible = false
        border.isVisible = borderVisible

        when (item) {
            is WalletConnectRequestViewItem.To -> bind(containerView.context.getString(R.string.TransactionInfo_To), item.value)
            is WalletConnectRequestViewItem.Input -> bind(containerView.context.getString(R.string.TransactionInfo_Input), item.value)
        }
    }

    private fun bind(title: String, value: String) {
        txtTitle.text = title
        decoratedText.text = value
        decoratedText.setOnClickListener {
            TextHelper.copyText(value)

            HudHelper.showSuccessMessage(containerView, R.string.Hud_Text_Copied)
        }
    }

}

package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_wallet_connect_request.*

class WalletConnectRequestFragment : BaseBottomSheetDialogFragment() {

    private val baseViewModel by activityViewModels<WalletConnectViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_wallet_connect_request)

        setTitle("title")
        setSubtitle("subtitle")
        setHeaderIcon(R.drawable.ic_confirm)

        val viewModel by viewModels<WalletConnectRequestViewModel> { WalletConnectRequestModule.Factory(baseViewModel.service, requireArguments().getLong(keyRequestId)) }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            dismiss()
        })

        btnApprove.setOnSingleClickListener {
            viewModel.approve()
        }

        btnReject.setOnSingleClickListener {
            viewModel.reject()
        }
    }

    companion object {
        val keyRequestId = "keyRequestId"

        fun newInstance(id: Long): WalletConnectRequestFragment {
            return WalletConnectRequestFragment().apply {
                arguments = bundleOf(keyRequestId to id)
            }
        }

    }

}
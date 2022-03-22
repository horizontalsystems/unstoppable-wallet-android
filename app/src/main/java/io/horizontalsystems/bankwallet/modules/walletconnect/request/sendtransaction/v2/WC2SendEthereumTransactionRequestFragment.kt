package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.ui.SendEthRequestScreen
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WC2SendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect v2")
    val vmFactory by lazy {
        WCRequestModule.FactoryV2(
            requireArguments().getLong(REQUEST_ID_KEY)
        )
    }
    private val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.wc2SendEthereumTransactionRequestFragment) { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                SendEthRequestScreen(
                    findNavController(),
                    viewModel,
                    sendEvmTransactionViewModel,
                    feeViewModel,
                    logger,
                    R.id.wc2SendEthereumTransactionRequestFragment
                ) { findNavController().popBackStack() }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
            viewModel.reject()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            findNavController().popBackStack()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

    }

    companion object {
        private const val REQUEST_ID_KEY = "request_id_key"

        fun prepareParams(@StringRes requestId: Long) =
            bundleOf(REQUEST_ID_KEY to requestId)
    }

}

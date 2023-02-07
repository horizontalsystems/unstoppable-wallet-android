package cash.p.terminal.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCRequestModule
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.ui.SendEthRequestScreen
import cash.p.terminal.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestScreen
import cash.p.terminal.modules.walletconnect.request.signmessage.v2.WC2UnsupportedRequestScreen
import cash.p.terminal.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import cash.p.terminal.modules.walletconnect.version2.WC2SignMessageRequest
import cash.p.terminal.modules.walletconnect.version2.WC2UnsupportedRequest
import io.horizontalsystems.core.helpers.HudHelper

class WC2RequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect v2")

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
                val requestId = requireArguments().getLong(REQUEST_ID_KEY)
                val navController = findNavController()
                val wc2RequestViewModel = viewModel<WC2RequestViewModel>(factory = WC2RequestViewModel.Factory(requestId))

                val requestData = wc2RequestViewModel.requestData
                when (requestData?.pendingRequest) {
                    is WC2UnsupportedRequest -> {
                        WC2UnsupportedRequestScreen(navController, requestData)
                    }
                    is WC2SignMessageRequest -> {
                        WC2SignMessageRequestScreen(navController, requestData)
                    }
                    is WC2SendEthereumTransactionRequest -> {
                        val vmFactory by lazy { WCRequestModule.FactoryV2(requestData) }
                        val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
                        val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
                        val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.wc2RequestFragment) { vmFactory }

                        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
                            viewModel.approve(transactionHash)
                            HudHelper.showSuccessMessage(
                                requireActivity().findViewById(android.R.id.content),
                                R.string.Hud_Text_Done
                            )
                            navController.popBackStack()
                        }

                        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
                            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
                        }

                        SendEthRequestScreen(
                            navController,
                            viewModel,
                            sendEvmTransactionViewModel,
                            feeViewModel,
                            logger,
                            R.id.wc2RequestFragment
                        ) { navController.popBackStack() }
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ID_KEY = "request_id_key"

        fun prepareParams(requestId: Long) =
            bundleOf(REQUEST_ID_KEY to requestId)
    }
}

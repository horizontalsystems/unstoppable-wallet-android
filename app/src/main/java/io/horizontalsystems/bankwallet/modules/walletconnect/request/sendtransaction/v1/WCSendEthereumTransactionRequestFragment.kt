package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.ui.SendEthRequestScreen
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WCSendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.wcSessionFragment)
    val vmFactory by lazy {
        WCRequestModule.Factory(
            baseViewModel.sharedSendEthereumTransactionRequest!!,
            baseViewModel.service,
            baseViewModel.dAppName
        )
    }
    private val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }
    private val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(R.id.wcSendEthereumTransactionRequestFragment) { vmFactory }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cacheNoneViewModel = nonceViewModel //Needed in SendEvmSettingsFragment
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
                    R.id.wcSendEthereumTransactionRequestFragment
                ) { close() }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

    }

    private fun close() {
        baseViewModel.sharedSendEthereumTransactionRequest = null
        findNavController().popBackStack()
    }

}

package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v1

import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.ui.SignMessageRequestScreen
import io.horizontalsystems.core.findNavController

class WCSignMessageRequestFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val baseViewModel = getBaseViewModel()

        if (baseViewModel == null) {
            findNavController().popBackStack()
            return
        }

        val vmFactory by lazy {
            WCSignMessageRequestModule.Factory(
                baseViewModel.sharedSignMessageRequest!!,
                baseViewModel.dAppName,
                baseViewModel.service
            )
        }
        val viewModel by viewModels<WCSignMessageRequestViewModel> { vmFactory }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            baseViewModel.sharedSignMessageRequest = null
            findNavController().popBackStack()
        }

        SignMessageRequestScreen(
            findNavController(),
            viewModel
        )
    }

    private fun getBaseViewModel(): WalletConnectViewModel? {
        return try {
            val viewModel by navGraphViewModels<WalletConnectViewModel>(R.id.wcSessionFragment)
            viewModel
        } catch (e: RuntimeException) {
            null
        }
    }

}

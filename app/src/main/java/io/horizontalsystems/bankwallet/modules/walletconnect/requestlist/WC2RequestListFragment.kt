package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.requestlist.ui.RequestListPage
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SignMessageRequest
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WC2RequestListFragment : BaseFragment() {

    private val viewModel by viewModels<WC2RequestListViewModel> {
        WC2RequestListModule.Factory()
    }

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
                RequestListPage(viewModel, findNavController())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.openRequestLiveEvent.observe(viewLifecycleOwner) { wcRequest ->
            when (wcRequest) {
                is WC2SignMessageRequest -> {
                    findNavController().slideFromBottom(
                        R.id.wc2RequestListFragment_to_wcSignMessageRequestFragment,
                        WC2SignMessageRequestFragment.prepareParams(wcRequest.id)
                    )
                }
                is WC2SendEthereumTransactionRequest -> {
                    findNavController().slideFromBottom(
                        R.id.wc2RequestListFragment_to_wcSendEthRequestFragment,
                        WC2SendEthereumTransactionRequestFragment.prepareParams(wcRequest.id)
                    )
                }
            }
        }

        viewModel.errorLiveEvent.observe(viewLifecycleOwner) { error ->
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), error)
        }
    }
}

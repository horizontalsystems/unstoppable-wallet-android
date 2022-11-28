package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SignMessageRequest
import io.horizontalsystems.core.findNavController

class WC2RequestFragment : BaseFragment() {

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

                WC2RequestScreen(requestId, findNavController())
            }
        }
    }

    companion object {
        private const val REQUEST_ID_KEY = "request_id_key"

        fun prepareParams(requestId: Long) =
            bundleOf(REQUEST_ID_KEY to requestId)
    }
}

@Composable
fun WC2RequestScreen(requestId: Long, navController: NavController) {
    val viewModel = viewModel<WC2RequestViewModel>(factory = WC2RequestViewModel.Factory(requestId))

    when (val pendingRequest = viewModel.requestData?.pendingRequest) {
        is WC2SignMessageRequest -> {
            navController.slideFromBottom(
                R.id.wc2SignMessageRequestFragment,
                WC2SignMessageRequestFragment.prepareParams(pendingRequest.id)
            )
        }
        is WC2SendEthereumTransactionRequest -> {
            navController.slideFromBottom(
                R.id.wc2SendEthereumTransactionRequestFragment,
                WC2SendEthereumTransactionRequestFragment.prepareParams(pendingRequest.id)
            )
        }
    }
}

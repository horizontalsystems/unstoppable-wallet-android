package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.ui.SignMessageRequestScreen
import io.horizontalsystems.core.findNavController

class WC2SignMessageRequestFragment : BaseFragment() {

    val vmFactory by lazy {
        WCSignMessageRequestModule.FactoryWC2(
            requireArguments().getLong(REQUEST_ID_KEY)
        )
    }
    private val viewModel by viewModels<WCSignMessageRequestViewModel> { vmFactory }

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
                SignMessageRequestScreen(
                    findNavController(),
                    viewModel
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    companion object {
        private const val REQUEST_ID_KEY = "request_id_key"

        fun prepareParams(requestId: Long) =
            bundleOf(REQUEST_ID_KEY to requestId)
    }

}

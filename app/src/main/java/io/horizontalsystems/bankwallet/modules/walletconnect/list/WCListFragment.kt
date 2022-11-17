package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WCListFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deepLinkUri = activity?.intent?.data?.toString()
        activity?.intent?.data = null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                WCSessionsScreen(
                    findNavController(),
                    deepLinkUri
                ) { connectUri -> handleConnectionUri(connectUri) }
            }
        }
    }

    private fun handleConnectionUri(connectUri: String) {
        val wcVersion: Int = WalletConnectListModule.getVersionFromUri(connectUri)
        if (wcVersion == 1) {
            findNavController().slideFromBottom(
                R.id.wcSessionFragment,
                WCSessionModule.prepareParams(null, connectUri)
            )
        } else if (wcVersion == 2) {
            App.wc2Service.pair(connectUri)
        } else {
            HudHelper.showErrorMessage(requireView(), R.string.WalletConnect_Error_InvalidUrl)
        }
    }

}

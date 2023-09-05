package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val deepLinkUri = activity?.intent?.data?.toString()
        activity?.intent?.data = null
        ComposeAppTheme {
            WCSessionsScreen(
                findNavController(),
                deepLinkUri
            )
        }
    }

}

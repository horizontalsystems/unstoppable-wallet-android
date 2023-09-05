package cash.p.terminal.modules.walletconnect.list

import androidx.compose.runtime.Composable
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.walletconnect.list.ui.WCSessionsScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
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

package cash.p.terminal.modules.walletconnect.list

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.walletconnect.list.ui.WCSessionsScreen

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val deepLinkUri = activity?.intent?.data?.toString()
        activity?.intent?.data = null
        WCSessionsScreen(
            navController,
            deepLinkUri
        )
    }

}

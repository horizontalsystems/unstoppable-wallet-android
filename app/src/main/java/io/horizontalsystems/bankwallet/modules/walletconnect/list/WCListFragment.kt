package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val deepLinkUri = activity?.intent?.data?.toString()
        activity?.intent?.data = null
        ComposeAppTheme {
            WCSessionsScreen(
                navController,
                deepLinkUri
            )
        }
    }

}

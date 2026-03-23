package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

class TonConnectSendRequestFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        TonConnectSendRequestScreen(navController)
    }
}

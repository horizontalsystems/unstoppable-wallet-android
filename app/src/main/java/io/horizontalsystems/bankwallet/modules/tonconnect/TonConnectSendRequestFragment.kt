package cash.p.terminal.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment

class TonConnectSendRequestFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TonConnectSendRequestScreen(navController)
    }
}

package cash.p.terminal.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.requireInput

class TonConnectNewFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TonConnectNewScreen(navController, navController.requireInput())
    }
}

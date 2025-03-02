package cash.p.terminal.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.requireInput

class TonConnectNewFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TonConnectNewScreen(navController, navController.requireInput())
    }
}

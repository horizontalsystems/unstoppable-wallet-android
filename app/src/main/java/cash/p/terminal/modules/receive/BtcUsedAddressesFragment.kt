package cash.p.terminal.modules.receive

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.modules.receive.ui.UsedAddressScreen
import cash.p.terminal.modules.receive.ui.UsedAddressesParams
import cash.p.terminal.ui_compose.BaseComposeFragment

class BtcUsedAddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<UsedAddressesParams>(navController) {
            UsedAddressScreen(it) { navController.popBackStack() }
        }
    }
}

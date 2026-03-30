package cash.p.terminal.modules.receive

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.modules.receive.ui.MoneroUsedAddressesScreen
import cash.p.terminal.modules.receive.viewmodels.MoneroUsedAddressesParams
import cash.p.terminal.ui_compose.BaseComposeFragment

class MoneroUsedAddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<MoneroUsedAddressesParams>(navController) { params ->
            MoneroUsedAddressesScreen(
                params = params,
                onBackPress = navController::navigateUp
            )
        }
    }
}

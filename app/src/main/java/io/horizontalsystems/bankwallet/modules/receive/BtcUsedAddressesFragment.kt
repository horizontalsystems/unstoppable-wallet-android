package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams
import kotlinx.serialization.Serializable

@Serializable
data class BtcUsedAddressesScreen(val params: UsedAddressesParams) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        UsedAddressScreen(params) { backStack.removeLastOrNull() }
    }
}

class BtcUsedAddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<UsedAddressesParams>(navController) {
            UsedAddressScreen(it) { navController.popBackStack() }
        }
    }
}

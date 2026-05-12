package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.bankwallet.modules.receive.ui.UsedAddressesParams
import kotlinx.serialization.Serializable

@Serializable
data class BtcUsedAddressesFragment(val input: UsedAddressesParams) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        UsedAddressScreen(input) { navController.removeLastOrNull() }
    }
}

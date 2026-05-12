package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class MoneroSubaddressesFragment(val input: SubaddressesParams) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        MoneroSubaddressesScreen(input) { navController.removeLastOrNull() }
    }
}

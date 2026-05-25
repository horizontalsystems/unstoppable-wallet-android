package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class MoneroSubaddressesPage(val input: SubaddressesParams) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        MoneroSubaddressesScreen(input) { navController.removeLastOrNull() }
    }
}

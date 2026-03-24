package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

class MoneroSubaddressesFragment(val input: SubaddressesParams) : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        MoneroSubaddressesScreen(input) { navController.removeLastOrNull() }
    }
}

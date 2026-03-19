package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.NavController

class MoneroSubaddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<SubaddressesParams>(navController) {
            MoneroSubaddressesScreen(it) { navController.popBackStack() }
        }
    }
}

package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class MoneroSubaddressesScreen(
    val subaddresses: List<SubaddressViewItem>
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        MoneroSubaddressesScreen(subaddresses) { backStack.removeLastOrNull() }
    }
}

class MoneroSubaddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        withInput<SubaddressesParams>(navController) {
//            MoneroSubaddressesScreen(it) { navController.popBackStack() }
//        }
    }
}

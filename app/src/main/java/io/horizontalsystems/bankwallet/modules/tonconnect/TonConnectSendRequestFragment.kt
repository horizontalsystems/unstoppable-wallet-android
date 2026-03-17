package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object TonConnectSendRequestScreen : HSScreen() {
    // TODO("Nav3: need to find other solution. There should not be mainActivityViewModel")
    lateinit var mainActivityViewModel: MainActivityViewModel

    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        TonConnectSendRequestScreen(backStack, mainActivityViewModel)
    }
}

class TonConnectSendRequestFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        TonConnectSendRequestScreen(navController)
    }
}

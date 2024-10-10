package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput

class TonConnectNewFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TonConnectNewScreen(navController, navController.requireInput())
    }
}

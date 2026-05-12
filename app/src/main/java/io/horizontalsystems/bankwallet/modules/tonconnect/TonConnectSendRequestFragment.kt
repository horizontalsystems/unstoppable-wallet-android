package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object TonConnectSendRequestFragment : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        TonConnectSendRequestScreen(navController)
    }
}

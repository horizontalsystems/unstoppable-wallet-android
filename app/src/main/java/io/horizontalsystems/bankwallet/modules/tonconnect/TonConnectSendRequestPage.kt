package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object TonConnectSendRequestPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        TonConnectSendRequestScreen(navigation)
    }
}

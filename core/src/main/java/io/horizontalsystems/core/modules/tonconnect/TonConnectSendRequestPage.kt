package io.horizontalsystems.core.modules.tonconnect

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object TonConnectSendRequestPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        TonConnectSendRequestScreen(navigation)
    }
}

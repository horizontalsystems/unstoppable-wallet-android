package io.horizontalsystems.walletkit.modules.zcashnetwork

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.zcashnetwork.addendpoint.AddZcashEndpointScreen
import kotlinx.serialization.Serializable

@Serializable
data object ZcashAddEndpointPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AddZcashEndpointScreen(navigation = navigation)
    }

}

package io.horizontalsystems.bankwallet.modules.zcashnetwork

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.zcashnetwork.addendpoint.AddZcashEndpointScreen
import kotlinx.serialization.Serializable

@Serializable
data object ZcashAddEndpointPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AddZcashEndpointScreen(navigation = navigation)
    }

}

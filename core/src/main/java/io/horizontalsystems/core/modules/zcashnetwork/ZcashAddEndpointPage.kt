package io.horizontalsystems.core.modules.zcashnetwork

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.zcashnetwork.addendpoint.AddZcashEndpointScreen
import kotlinx.serialization.Serializable

@Serializable
data object ZcashAddEndpointPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AddZcashEndpointScreen(navigation = navigation)
    }

}

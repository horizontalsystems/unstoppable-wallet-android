package io.horizontalsystems.core.modules.receive.monero

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class MoneroSubaddressesPage(val input: SubaddressesParams) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        MoneroSubaddressesScreen(input) { navigation.removeLastOrNull() }
    }
}

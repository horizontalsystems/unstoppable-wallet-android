package io.horizontalsystems.core.modules.receive

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.core.modules.receive.ui.UsedAddressesParams
import kotlinx.serialization.Serializable

@Serializable
data class BtcUsedAddressesPage(val input: UsedAddressesParams) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        UsedAddressScreen(input) { navigation.removeLastOrNull() }
    }
}

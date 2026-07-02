package io.horizontalsystems.walletkit.modules.receive

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.ui.UsedAddressScreen
import io.horizontalsystems.walletkit.modules.receive.ui.UsedAddressesParams
import kotlinx.serialization.Serializable

@Serializable
data class BtcUsedAddressesPage(val input: UsedAddressesParams) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        UsedAddressScreen(input) { navigation.removeLastOrNull() }
    }
}

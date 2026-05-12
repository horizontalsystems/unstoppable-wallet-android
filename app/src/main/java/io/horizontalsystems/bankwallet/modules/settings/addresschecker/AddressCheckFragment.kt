package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.UnifiedAddressCheckScreen
import kotlinx.serialization.Serializable

@Serializable
data object AddressCheckFragment : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        UnifiedAddressCheckScreen(
            onClose = { navController.removeLastOrNull() }
        )
    }
}

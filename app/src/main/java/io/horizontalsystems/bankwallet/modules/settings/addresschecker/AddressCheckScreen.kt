package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.UnifiedAddressCheckScreen
import kotlinx.serialization.Serializable

@Serializable
data object AddressCheckScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        UnifiedAddressCheckScreen(
            onClose = { backStack.removeLastOrNull() }
        )
    }
}

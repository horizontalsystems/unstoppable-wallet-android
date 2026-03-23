package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.UnifiedAddressCheckScreen

class AddressCheckFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        UnifiedAddressCheckScreen(
            onClose = { navController.removeLastOrNull() }
        )
    }
}

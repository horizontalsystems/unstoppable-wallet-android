package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.UnifiedAddressCheckScreen

class AddressCheckFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        UnifiedAddressCheckScreen(
            onClose = { navController.popBackStack() }
        )
    }
}

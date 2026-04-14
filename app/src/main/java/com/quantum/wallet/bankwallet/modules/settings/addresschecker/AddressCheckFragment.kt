package com.quantum.wallet.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.settings.addresschecker.ui.UnifiedAddressCheckScreen

class AddressCheckFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        UnifiedAddressCheckScreen(
            onClose = { navController.popBackStack() }
        )
    }
}

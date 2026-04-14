package com.quantum.wallet.bankwallet.modules.receive

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.receive.ui.UsedAddressScreen
import com.quantum.wallet.bankwallet.modules.receive.ui.UsedAddressesParams

class BtcUsedAddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<UsedAddressesParams>(navController) {
            UsedAddressScreen(it) { navController.popBackStack() }
        }
    }
}

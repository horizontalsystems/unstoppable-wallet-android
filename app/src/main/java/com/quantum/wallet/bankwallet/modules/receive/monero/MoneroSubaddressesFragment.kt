package com.quantum.wallet.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment

class MoneroSubaddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<SubaddressesParams>(navController) {
            MoneroSubaddressesScreen(it) { navController.popBackStack() }
        }
    }
}

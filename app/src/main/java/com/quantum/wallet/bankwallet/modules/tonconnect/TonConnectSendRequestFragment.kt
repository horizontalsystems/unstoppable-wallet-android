package com.quantum.wallet.bankwallet.modules.tonconnect

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.BaseComposeFragment

class TonConnectSendRequestFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TonConnectSendRequestScreen(navController)
    }
}

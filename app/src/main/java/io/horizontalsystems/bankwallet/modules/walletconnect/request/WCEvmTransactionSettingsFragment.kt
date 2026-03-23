package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel

class WCEvmTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        WCEvmTransactionSettingsScreen(navController)
    }
}

@Composable
fun WCEvmTransactionSettingsScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = navController.viewModelForScreen<WCSendEthereumTransactionRequestViewModel>(WCRequestFragment::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

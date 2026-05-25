package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import kotlinx.serialization.Serializable

@Serializable
data object WCEvmTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        WCEvmTransactionSettingsScreen(navController)
    }
}

@Composable
fun WCEvmTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForScreen<WCSendEthereumTransactionRequestViewModel>(WCRequestSheet::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

package io.horizontalsystems.walletkit.modules.walletconnect.request

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import kotlinx.serialization.Serializable

@Serializable
data object WCEvmTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        WCEvmTransactionSettingsScreen(navigation)
    }
}

@Composable
fun WCEvmTransactionSettingsScreen(navigation: HSNavigation) {
    val viewModel = navigation.viewModelForScreen<WCSendEthereumTransactionRequestViewModel>(WCRequestSheet::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navigation)
}

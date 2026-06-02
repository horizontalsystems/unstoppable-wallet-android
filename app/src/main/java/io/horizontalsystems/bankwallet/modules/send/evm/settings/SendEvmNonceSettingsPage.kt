package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationPage
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SendEvmNonceSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        NonceSettingsScreen(navController)
    }
}

@Composable
fun NonceSettingsScreen(navController: HSNavigation) {
    val viewModel = viewModelForScreen<SendEvmConfirmationViewModel>(SendEvmConfirmationPage::class)
    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}

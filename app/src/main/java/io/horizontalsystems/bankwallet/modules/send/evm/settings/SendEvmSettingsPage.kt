package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationPage
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import kotlinx.serialization.Serializable
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen

@Serializable
data object SendEvmSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SendEvmSettingsScreen(navController)
    }
}

@Composable
fun SendEvmSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForScreen<SendEvmConfirmationViewModel>(SendEvmConfirmationPage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

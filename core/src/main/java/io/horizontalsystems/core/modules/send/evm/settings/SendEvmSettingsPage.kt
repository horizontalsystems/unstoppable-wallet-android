package io.horizontalsystems.core.modules.send.evm.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.send.evm.confirmation.SendEvmConfirmationPage
import io.horizontalsystems.core.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SendEvmSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SendEvmSettingsScreen(navigation)
    }
}

@Composable
fun SendEvmSettingsScreen(navigation: HSNavigation) {
    val viewModel = navigation.viewModelForScreen<SendEvmConfirmationViewModel>(SendEvmConfirmationPage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navigation)
}

package io.horizontalsystems.core.modules.opencryptopay

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object OpenCryptoPayEvmSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<OpenCryptoPayEvmConfirmationViewModel>(
            OpenCryptoPayEvmConfirmationPage::class
        )
        viewModel.sendTransactionService.GetSettingsContent(navigation)
    }
}

@Serializable
data object OpenCryptoPayEvmNonceSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<OpenCryptoPayEvmConfirmationViewModel>(
            OpenCryptoPayEvmConfirmationPage::class
        )
        viewModel.sendTransactionService.GetNonceSettingsContent(navigation)
    }
}

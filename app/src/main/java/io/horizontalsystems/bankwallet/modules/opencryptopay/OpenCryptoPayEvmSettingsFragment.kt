package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object OpenCryptoPayEvmSettingsFragment : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<OpenCryptoPayEvmConfirmationViewModel>(
            OpenCryptoPayEvmConfirmationFragment::class
        )
        viewModel.sendTransactionService.GetSettingsContent(navController)
    }
}

@Serializable
data object OpenCryptoPayEvmNonceSettingsFragment : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = navController.viewModelForScreen<OpenCryptoPayEvmConfirmationViewModel>(
            OpenCryptoPayEvmConfirmationFragment::class
        )
        viewModel.sendTransactionService.GetNonceSettingsContent(navController)
    }
}

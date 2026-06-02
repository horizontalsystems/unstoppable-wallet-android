package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen

@Serializable
data class SwapTransactionNonceSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapTransactionNonceSettingsScreen(navController, parentScreenContentKey)
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(
    navController: HSNavigation,
    parentScreenContentKey: String
) {
    val viewModel = navController.viewModelForScreen<SwapConfirmViewModel>(parentScreenContentKey)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}

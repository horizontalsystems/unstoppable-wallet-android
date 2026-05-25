package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class SwapTransactionSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapTransactionSettingsScreen(navController, parentScreenContentKey)
    }
}

@Composable
fun SwapTransactionSettingsScreen(navController: HSNavigation, parentScreenContentKey: String) {
    val viewModel = navController.viewModelForScreen<SwapConfirmViewModel>(parentScreenContentKey)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

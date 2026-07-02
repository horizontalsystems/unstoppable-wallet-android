package io.horizontalsystems.walletkit.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class SwapTransactionSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapTransactionSettingsScreen(navigation, parentScreenContentKey)
    }
}

@Composable
fun SwapTransactionSettingsScreen(navigation: HSNavigation, parentScreenContentKey: String) {
    val viewModel = navigation.viewModelForScreen<SwapConfirmViewModel>(parentScreenContentKey)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navigation)
}

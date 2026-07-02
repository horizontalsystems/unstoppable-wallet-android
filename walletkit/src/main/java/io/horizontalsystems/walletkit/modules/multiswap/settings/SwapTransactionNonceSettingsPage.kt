package io.horizontalsystems.walletkit.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class SwapTransactionNonceSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapTransactionNonceSettingsScreen(navigation, parentScreenContentKey)
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(
    navigation: HSNavigation,
    parentScreenContentKey: String
) {
    val viewModel = navigation.viewModelForScreen<SwapConfirmViewModel>(parentScreenContentKey)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navigation)
}

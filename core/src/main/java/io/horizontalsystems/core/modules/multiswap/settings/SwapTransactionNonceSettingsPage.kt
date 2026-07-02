package io.horizontalsystems.core.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
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

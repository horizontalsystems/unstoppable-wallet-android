package io.horizontalsystems.core.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
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

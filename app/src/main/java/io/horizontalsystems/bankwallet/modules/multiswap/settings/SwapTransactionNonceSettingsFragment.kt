package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.multiswap.SwapConfirmViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object SwapTransactionNonceSettingsFragment : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapTransactionNonceSettingsScreen(navController)
    }
}

@Composable
fun SwapTransactionNonceSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForPrevScreen<SwapConfirmViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}

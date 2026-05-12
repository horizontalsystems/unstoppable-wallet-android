package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable

@Serializable
data class SwapSelectCoinFragment(val input: Input) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapSelectCoinScreen(navController, input.token, input.title)
    }

    @Serializable
    data class Input(val token: Token?, val title: String)

}

@Composable
private fun SwapSelectCoinScreen(
    navController: HSNavigation,
    token: Token?,
    title: String?
) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel = viewModel<SwapSelectCoinViewModel>(
        factory = SwapSelectCoinViewModel.Factory(token)
    )
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        title = title ?: "",
        coinBalanceItems = uiState.coinBalanceItems,
        onSearchTextChanged = viewModel::setQuery,
        onClose = navController::removeLastOrNull
    ) {
        resultEventBus.sendResult(it.token)
        navController.removeLastOrNull()
    }
}

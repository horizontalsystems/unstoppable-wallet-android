package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable

@Serializable
data class SwapSelectCoinPage(val input: Input) : HSPage() {
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
    val viewModel = hiltViewModel<SwapSelectCoinViewModel, SwapSelectCoinViewModel.Factory> { factory ->
        factory.create(token)
    }
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        title = title ?: "",
        uiState = uiState,
        onSearchTextChanged = viewModel::setQuery,
        onClose = navController::removeLastOrNull,
        onRecordRecent = { viewModel.onRecentTokenSelected(it.token) }
    ) {
        resultEventBus.sendResult(it.token)
        navController.removeLastOrNull()
    }
}

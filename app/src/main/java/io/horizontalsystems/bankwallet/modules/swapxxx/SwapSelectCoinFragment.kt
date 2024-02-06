package cash.p.terminal.modules.swapxxx

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.swap.coinselect.SelectSwapCoinDialogScreen

class SwapSelectCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapSelectCoinScreen(navController)
    }
}

@Composable
private fun SwapSelectCoinScreen(navController: NavController) {
    val viewModel = viewModel<SwapSelectCoinViewModel>(factory = SwapSelectCoinViewModel.Factory())
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        coinBalanceItems = uiState.coinBalanceItems,
        onSearchTextChanged = viewModel::setQuery,
        onClose = navController::popBackStack
    ) {
        navController.setNavigationResultX(it.token)
        navController.popBackStack()
    }
}

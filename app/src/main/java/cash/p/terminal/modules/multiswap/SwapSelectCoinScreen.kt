package cash.p.terminal.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject

@Composable
fun SwapSelectCoinScreen(
    navController: NavController,
    token: Token?,
    title: String?,
    onSelect: (Token) -> Unit
) {
    val accountManager: IAccountManager by inject(IAccountManager::class.java)
    val activeAccount = accountManager.activeAccount ?: return

    val viewModel = koinViewModel<SwapSelectCoinViewModel> {
        parametersOf(token, activeAccount)
    }
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        title = title ?: "",
        coinBalanceItems = uiState.coinBalanceItems,
        loading = uiState.loading,
        onSearchTextChanged = viewModel::setQuery,
        onClose = navController::popBackStack
    ) {
        onSelect(it.token)
    }
}

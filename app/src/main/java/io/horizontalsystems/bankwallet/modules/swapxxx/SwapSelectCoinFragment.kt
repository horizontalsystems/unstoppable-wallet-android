package cash.p.terminal.modules.swapxxx

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.requireInput
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.swap.coinselect.SelectSwapCoinDialogScreen
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SwapSelectCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        SwapSelectCoinScreen(navController, input.otherSelectedToken)
    }

    @Parcelize
    data class Input(val otherSelectedToken: Token?) : Parcelable
}

@Composable
private fun SwapSelectCoinScreen(navController: NavController, otherSelectedToken: Token?) {
    val viewModel = viewModel<SwapSelectCoinViewModel>(
        factory = SwapSelectCoinViewModel.Factory(otherSelectedToken)
    )
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

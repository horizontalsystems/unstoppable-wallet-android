package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapSelectCoinScreen(
    @Serializable(with = TokenSerializer::class)
    val token: Token?,
    val title: String
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        SwapSelectCoinScreen(backStack, resultBus, token, title)
    }

    data class Result(val token: Token)
}

class SwapSelectCoinFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(val token: Token?, val title: String) : Parcelable

}

@Composable
private fun SwapSelectCoinScreen(
    backStack: NavBackStack<HSScreen>,
    resultBus: ResultEventBus,
    token: Token?,
    title: String?
) {
    val viewModel = viewModel<SwapSelectCoinViewModel>(
        factory = SwapSelectCoinViewModel.Factory(token)
    )
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        title = title ?: "",
        coinBalanceItems = uiState.coinBalanceItems,
        onSearchTextChanged = viewModel::setQuery,
        onClose = backStack::removeLastOrNull
    ) {
        resultBus.sendResult(result = SwapSelectCoinScreen.Result(it.token))
        backStack.removeLastOrNull()
    }
}

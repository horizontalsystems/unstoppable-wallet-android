package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapSelectCoinScreen(
    @Serializable(with = TokenSerializer::class)
    val otherSelectedToken: Token?,
    val title: String,
    val type: Type
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        SwapSelectCoinScreen(
            backStack,
            otherSelectedToken,
            title,
            type
        )
    }

    data class Result(val token: Token, val type: Type)

    enum class Type {
        In, Out
    }
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
    otherSelectedToken: Token?,
    title: String?,
    type: SwapSelectCoinScreen.Type
) {
    val resultBus = LocalResultEventBus.current
    val viewModel = viewModel<SwapSelectCoinViewModel>(
        factory = SwapSelectCoinViewModel.Factory(otherSelectedToken)
    )
    val uiState = viewModel.uiState

    SelectSwapCoinDialogScreen(
        title = title ?: "",
        coinBalanceItems = uiState.coinBalanceItems,
        onSearchTextChanged = viewModel::setQuery,
        onClose = backStack::removeLastOrNull
    ) {
        resultBus.sendResult(result = SwapSelectCoinScreen.Result(it.token, type))
        backStack.removeLastOrNull()
    }
}

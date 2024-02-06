package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.eligibleTokens
import io.horizontalsystems.bankwallet.modules.receive.FullCoinsProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwapSelectCoinViewModel : ViewModel() {
    private val activeAccount = App.accountManager.activeAccount!!
    private val coinsProvider = FullCoinsProvider(App.marketKit, activeAccount)

    private var coinBalanceItems = listOf<SwapMainModule.CoinBalanceItem>()

    var uiState by mutableStateOf(
        SwapSelectCoinUiState(
            coinBalanceItems = coinBalanceItems
        )
    )

    init {
        coinsProvider.setActiveWallets(App.walletManager.activeWallets)
        viewModelScope.launch {
            reloadItems()
            emitState()
        }
    }

    fun setQuery(q: String) {
        coinsProvider.setQuery(q)
        viewModelScope.launch {
            reloadItems()
            emitState()
        }
    }

    private suspend fun reloadItems() = withContext(Dispatchers.Default) {
        coinBalanceItems = coinsProvider.getItems()
            .map { it.eligibleTokens(activeAccount.type) }
            .flatten()
            .map {
                SwapMainModule.CoinBalanceItem(it, null, null)
            }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapSelectCoinUiState(
                coinBalanceItems = coinBalanceItems
            )
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapSelectCoinViewModel() as T
        }
    }
}

data class SwapSelectCoinUiState(val coinBalanceItems: List<SwapMainModule.CoinBalanceItem>)

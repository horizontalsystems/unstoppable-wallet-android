package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.CoinBalanceItem

object SelectSwapCoinModule {

    class Factory(private val coinBalanceItems: List<CoinBalanceItem>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectSwapCoinViewModel(coinBalanceItems) as T
        }
    }

}

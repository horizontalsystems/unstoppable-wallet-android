package io.horizontalsystems.bankwallet.modules.swap_new.coinselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem

object SelectSwapCoinModule {

    class Factory(private val coinBalanceItems: List<CoinBalanceItem>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectSwapCoinViewModel(coinBalanceItems) as T
        }
    }

}

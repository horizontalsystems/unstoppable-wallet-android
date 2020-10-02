package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin

object SelectSwapCoinModule {

    class Factory(private val excludedCoin: Coin?, private val hideZeroBalance: Boolean?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectSwapCoinViewModel(excludedCoin, hideZeroBalance, App.coinManager, App.walletManager, App.adapterManager) as T
        }
    }

}

package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.receive.FullCoinsProvider

object SelectSwapCoinModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val coinProvider = SwapCoinProvider(
                App.walletManager,
                App.adapterManager,
                App.currencyManager,
                App.marketKit,
                FullCoinsProvider(App.marketKit, App.accountManager.activeAccount!!)
            )
            return SelectSwapCoinViewModel(coinProvider) as T
        }
    }

}

package cash.p.terminal.modules.swap.coinselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.receive.FullCoinsProvider

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

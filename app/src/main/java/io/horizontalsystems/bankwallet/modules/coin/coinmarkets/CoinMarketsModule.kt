package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.FullCoin

object CoinMarketsModule {
    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CoinMarketsViewModel(fullCoin.coin.code, fullCoin.coin.uid, App.currencyManager, App.marketKit, App.numberFormatter) as T
        }
    }
}

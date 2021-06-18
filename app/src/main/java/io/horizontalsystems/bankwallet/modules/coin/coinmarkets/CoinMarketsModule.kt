package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object CoinMarketsModule {

    class Factory(private val coinCode: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CoinMarketsViewModel(coinCode, App.numberFormatter, App.appConfigProvider) as T
        }

    }
}

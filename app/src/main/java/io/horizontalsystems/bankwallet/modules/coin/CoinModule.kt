package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object CoinModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val fullCoin = App.marketKit.fullCoins(coinUids = listOf(coinUid)).first()
            val service = CoinService(fullCoin, App.marketFavoritesManager)
            return CoinViewModel(service, listOf(service), App.localStorage, App.subscriptionManager) as T
        }

    }

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.Coin_Tab_Overview),
        Details(R.string.Coin_Tab_Details),
        Market(R.string.Coin_Tab_Market),
//        Tweets(R.string.Coin_Tab_Tweets);
    }
}

package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object CoinModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = CoinService(coinUid, App.coinManager, App.marketFavoritesManager, App.walletManager, App.accountManager)
            return CoinViewModel(service, listOf(service)) as T
        }

    }

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.Coin_Tab_Overview),
        Market(R.string.Coin_Tab_Market),
        Details(R.string.Coin_Tab_Details),
        Tweets(R.string.Coin_Tab_Tweets);
    }
}

enum class CoinState {
    InWallet, AddedToWallet, NotInWallet, NoActiveAccount, Unsupported
}

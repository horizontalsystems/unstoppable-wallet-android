package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinType

object CoinModule {

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinUid: String, private val coinCode: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val service = CoinService(
                    coinType,
                    coinUid,
                    currency,
                    App.xRateManager,
                    App.marketKit,
                    App.chartTypeStorage,
                    App.priceAlertManager,
                    App.notificationManager,
                    App.marketFavoritesManager,
                    App.appConfigProvider.guidesUrl
            )
            return CoinViewModel(service, coinCode, coinTitle, CoinViewFactory(currency, App.numberFormatter), listOf(service)) as T
        }

    }
}

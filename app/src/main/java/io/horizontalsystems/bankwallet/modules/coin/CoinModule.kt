package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType
import java.math.BigDecimal

object CoinModule {

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinCode: String, private val coinId: String?) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)
            val coinService = CoinService(coinType, currency, App.xRateManager, App.chartTypeStorage, App.priceAlertManager, App.notificationManager, App.localStorage, App.marketFavoritesManager)
            return CoinViewModel(rateFormatter, coinService, coinCode, coinTitle, coinId, RateChartViewFactory()) as T
        }

    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)
}

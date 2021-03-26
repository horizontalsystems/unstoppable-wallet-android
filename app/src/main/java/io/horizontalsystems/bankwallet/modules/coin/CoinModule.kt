package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.CoinPlatformType
import java.math.BigDecimal

object CoinModule {

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinCode: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)
            val service = CoinService(coinType, currency, App.xRateManager, App.chartTypeStorage, App.priceAlertManager, App.notificationManager, App.localStorage, App.marketFavoritesManager, App.appConfigProvider.guidesUrl)
            return CoinViewModel(rateFormatter, service, coinCode, coinTitle, CoinViewFactory(currency, App.numberFormatter), listOf(service)) as T
        }

    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)
}

data class MarketTickerViewItem(
        val title: String,
        val subtitle: String,
        val value: String,
        val subvalue: String,
) {
    fun areItemsTheSame(other: MarketTickerViewItem): Boolean {
        return title == other.title && subtitle == other.subvalue
    }

    fun areContentsTheSame(other: MarketTickerViewItem): Boolean {
        return this == other
    }
}

sealed class CoinExtraPage {
    class Markets(val position: ListPosition) : CoinExtraPage()
    class Investors(val position: ListPosition) : CoinExtraPage()
}

sealed class InvestorItem {
    data class Header(val title: String) : InvestorItem()
    data class Fund(val name: String, val url: String, val cleanedUrl: String, val position: ListPosition) : InvestorItem()
}

val CoinPlatformType.title: String
    get() = when (this) {
        CoinPlatformType.OTHER -> "Other"
        CoinPlatformType.ETHEREUM -> "ETH Contract"
        CoinPlatformType.BINANCE -> "Binance DEX Contract"
        CoinPlatformType.BINANCE_SMART_CHAIN -> "BSC Contract"
        CoinPlatformType.TRON -> "TRON Contract"
        CoinPlatformType.EOS -> "EOS Contract"
    }

val CoinPlatformType.order: Int
    get() = when (this) {
        CoinPlatformType.ETHEREUM -> 1
        CoinPlatformType.BINANCE_SMART_CHAIN -> 2
        CoinPlatformType.BINANCE -> 3
        CoinPlatformType.TRON -> 4
        CoinPlatformType.EOS -> 5
        CoinPlatformType.OTHER -> 6
    }

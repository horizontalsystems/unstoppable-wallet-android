package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.CoinPlatformType

object CoinModule {

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinCode: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val service = CoinService(
                    coinType,
                    currency,
                    App.xRateManager,
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

data class MarketTickerViewItem(
        val title: String,
        val subtitle: String,
        val value: String,
        val subvalue: String,
        val imageUrl: String?,
) {
    fun areItemsTheSame(other: MarketTickerViewItem): Boolean {
        return title == other.title && subtitle == other.subvalue
    }

    fun areContentsTheSame(other: MarketTickerViewItem): Boolean {
        return this == other
    }
}

sealed class CoinExtraPage {
    class TradingVolume(val position: ListPosition, val value: String?, val canShowMarkets: Boolean) : CoinExtraPage()
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

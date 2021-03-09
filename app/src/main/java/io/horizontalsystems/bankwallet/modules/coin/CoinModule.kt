package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.xrateskit.entities.*
import java.math.BigDecimal

object CoinModule {

    interface View {
        fun chartSpinner(isLoading: Boolean)
        fun marketSpinner(isLoading: Boolean)
        fun setChartType(type: ChartType)
        fun showChartInfo(viewItem: ChartInfoViewItem)
        fun showMarketInfo(viewItem: MarketInfoViewItem)
        fun showSelectedPointInfo(item: ChartPointViewItem)
        fun showError(ex: Throwable)
        fun setEmaEnabled(enabled: Boolean)
        fun setMacdEnabled(enabled: Boolean)
        fun setRsiEnabled(enabled: Boolean)
        fun notificationIconUpdated()
        fun openNotificationMenu(coinId: String, coinName: String)
        fun setIsFavorite(value: Boolean)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(type: ChartType)
        fun onTouchSelect(point: PointInfo)
        fun toggleEma()
        fun toggleMacd()
        fun toggleRsi()
        fun onNotificationClick()
        fun onFavoriteClick()
        fun onUnfavoriteClick()
    }

    interface Interactor {
        var defaultChartType: ChartType?
        val notificationsAreEnabled: Boolean

        fun getMarketInfo(coinType: CoinType, currencyCode: String): MarketInfo?
        fun getChartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType): ChartInfo?
        fun getCoinDetails(coinType: CoinType, currencyCode: String, rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>)
        fun observeChartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType)
        fun observeMarketInfo(coinType: CoinType, currencyCode: String)
        fun clear()
        fun observeAlertNotification()
        fun getPriceAlert(coinCode: String): PriceAlert
        fun isCoinFavorite(coinType: CoinType): Boolean
        fun favorite(coinType: CoinType)
        fun unfavorite(coinType: CoinType)
    }

    interface InteractorDelegate {
        fun onUpdate(chartInfo: ChartInfo)
        fun onUpdate(marketInfo: MarketInfo)
        fun onUpdate(coinMarketDetails: CoinMarketDetails)
        fun onChartError(ex: Throwable)
        fun onMarketError(ex: Throwable)
        fun updateAlertNotificationIconState()
        fun updateFavoriteNotificationItemState()
    }

    interface Router

    class Factory(private val coinTitle: String, private val coinType: CoinType, private val coinCode: String, private val coinId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)

            val view = CoinView()
            val interactor = CoinInteractor(App.xRateManager, App.chartTypeStorage, App.priceAlertManager, App.notificationManager, App.localStorage, App.marketFavoritesManager)
            val presenter = CoinPresenter(view, rateFormatter, interactor, coinType, coinCode, coinTitle, coinId, currency, RateChartViewFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)
}

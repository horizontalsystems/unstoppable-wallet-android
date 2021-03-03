package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.*

class RateChartPresenter(
        val view: View,
        val rateFormatter: RateFormatter,
        private val interactor: Interactor,
        private val coinType: CoinType,
        private val coinCode: String,
        private val coinTitle: String,
        private val coinId: String?,
        private val currency: Currency,
        private val factory: RateChartViewFactory)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    var notificationIconVisible = coinId != null && interactor.notificationsAreEnabled
    var notificationIconActive = false

    private var chartType = interactor.defaultChartType ?: ChartType.TODAY
    private var emaIsEnabled = false
    private var macdIsEnabled = false
    private var rsiIsEnabled = false

    private var chartInfo: ChartInfo? = null
        set(value) {
            field = value
            updateChartInfo()
        }

    private var marketInfo: MarketInfo? = null
        set(value) {
            field = value
            updateMarketInfo()
        }

    private var coinMarketDetails: CoinMarketDetails? = null
        set(value) {
            field = value
            updateMarketInfo()
        }

    //  ViewDelegate

    override fun viewDidLoad() {
        view.setChartType(chartType)

        marketInfo = interactor.getMarketInfo(coinType, currency.code)
        interactor.observeMarketInfo(coinType, currency.code)
        interactor.observeAlertNotification()

        fetchChartInfo()
        updateAlertNotificationIconState()

        updateFavoriteNotificationItemState()
    }

    override fun onSelect(type: ChartType) {
        if (chartType == type)
            return

        chartType = type
        interactor.defaultChartType = type

        fetchChartInfo()
    }

    override fun onTouchSelect(point: PointInfo) {
        val price = CurrencyValue(currency, point.value.toBigDecimal())

        if (macdIsEnabled) {
            view.showSelectedPointInfo(ChartPointViewItem(point.timestamp, price, null, point.macdInfo))
        } else {
            val volume = point.volume?.let { volume ->
                CurrencyValue(currency, volume.toBigDecimal())
            }
            view.showSelectedPointInfo(ChartPointViewItem(point.timestamp, price, volume, null))
        }
    }

    override fun onNotificationClick() {
        coinId?.let {
            view.openNotificationMenu(it, coinTitle)
        }
    }

    override fun onFavoriteClick() {
        interactor.favorite(coinType)
    }

    override fun onUnfavoriteClick() {
        interactor.unfavorite(coinType)
    }

    override fun toggleEma() {
        emaIsEnabled = !emaIsEnabled
        view.setEmaEnabled(emaIsEnabled)
    }

    override fun toggleMacd() {
        if (rsiIsEnabled) {
            toggleRsi()
        }

        macdIsEnabled = !macdIsEnabled
        view.setMacdEnabled(macdIsEnabled)
    }

    override fun toggleRsi() {
        if (macdIsEnabled) {
            toggleMacd()
        }

        rsiIsEnabled = !rsiIsEnabled
        view.setRsiEnabled(rsiIsEnabled)
    }

    override fun updateAlertNotificationIconState() {
        val coinId = coinId ?: return
        val priceAlert = interactor.getPriceAlert(coinId)
        notificationIconActive = priceAlert.changeState != PriceAlert.ChangeState.OFF || priceAlert.trendState != PriceAlert.TrendState.OFF
        view.notificationIconUpdated()
    }

    override fun updateFavoriteNotificationItemState() {
        view.setIsFavorite(interactor.isCoinFavorite(coinType))
    }

    private fun fetchChartInfo() {
        view.chartSpinner(isLoading = true)

        chartInfo = interactor.getChartInfo(coinType, currency.code, chartType)

        interactor.observeChartInfo(coinType, currency.code, chartType)
        interactor.getCoinDetails(coinType, currency.code, listOf("USD", "BTC", "ETH"), listOf(TimePeriod.DAY_7, TimePeriod.DAY_30))
    }

    private fun updateMarketInfo() {
        val marketInfo = marketInfo ?: return
        val marketDetails = coinMarketDetails ?: return

        view.marketSpinner(isLoading = false)
        view.showMarketInfo(factory.createMarketInfo(marketInfo, marketDetails, currency, coinCode))

        val info = chartInfo ?: return
        try {
            view.showChartInfo(factory.createChartInfo(chartType, info, marketInfo))
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    private fun updateChartInfo() {
        val info = chartInfo ?: return

        view.chartSpinner(isLoading = false)

        try {
            view.showChartInfo(factory.createChartInfo(chartType, info, marketInfo))
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    //  InteractorDelegate

    override fun onUpdate(marketInfo: MarketInfo) {
        this.marketInfo = marketInfo
    }

    override fun onUpdate(chartInfo: ChartInfo) {
        this.chartInfo = chartInfo
    }

    override fun onUpdate(coinMarketDetails: CoinMarketDetails) {
        this.coinMarketDetails = coinMarketDetails
    }

    override fun onChartError(ex: Throwable) {
        view.chartSpinner(isLoading = false)
        view.showError(ex)
    }

    override fun onMarketError(ex: Throwable) {
        view.marketSpinner(isLoading = false)
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

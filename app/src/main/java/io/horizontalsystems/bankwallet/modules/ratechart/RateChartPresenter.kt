package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateChartPresenter(
        val view: View,
        val rateFormatter: RateFormatter,
        private val interactor: Interactor,
        private val coin: Coin,
        private val currency: Currency,
        private val factory: RateChartViewFactory)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    private var chartType = interactor.defaultChartType ?: ChartType.DAILY

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

    //  ViewDelegate

    override fun viewDidLoad() {
        view.setChartType(chartType)

        marketInfo = interactor.getMarketInfo(coin.code, currency.code)
        interactor.observeMarketInfo(coin.code, currency.code)

        fetchChartInfo()
    }

    override fun onSelect(type: ChartType) {
        if (chartType == type)
            return

        chartType = type
        interactor.defaultChartType = type

        fetchChartInfo()
    }

    override fun onTouchSelect(point: ChartPoint) {
        val currencyValue = CurrencyValue(currency, point.value.toBigDecimal())
        view.showSelectedPoint(Triple(point.timestamp, currencyValue, chartType))
    }

    private fun fetchChartInfo() {
        view.showSpinner()

        chartInfo = interactor.getChartInfo(coin.code, currency.code, chartType)
        interactor.observeChartInfo(coin.code, currency.code, chartType)
    }

    private fun updateMarketInfo() {
        marketInfo?.let {
            val viewItem = factory.createMarketInfo(it, currency, coin)
            view.showMarketInfo(viewItem)
        }
    }

    private fun updateChartInfo() {
        val info = chartInfo ?: return

        view.hideSpinner()

        try {
            val viewItem = factory.createChartInfo(chartType, info)
            view.showChartInfo(viewItem)
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

    override fun onError(ex: Throwable) {
        view.hideSpinner()
        view.showError(ex)
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateChartPresenter(
        val view: View,
        private val interactor: Interactor,
        private val coinCode: CoinCode,
        private val currency: Currency,
        private val factory: RateChartViewFactory)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    private var chartType = interactor.defaultChartType ?: ChartType.DAILY

    private var chartInfo: ChartInfo? = null
        set(value) {
            field = value
            updateChart()
        }

    private var marketInfo: MarketInfo? = null
        set(value) {
            field = value
            updateChart()
        }

    //  ViewDelegate

    override fun viewDidLoad() {
        view.showSpinner()
        view.setChartType(chartType)

        fetchChartData()
    }

    override fun onSelect(type: ChartType) {
        if (chartType == type)
            return

        chartType = type
        interactor.defaultChartType = type

        view.showSpinner()
        fetchChartData()
    }

    private fun fetchChartData() {
        interactor.clear()

        marketInfo = interactor.getMarketInfo(coinCode, currency.code)
        interactor.observeMarketInfo(coinCode, currency.code)

        chartInfo = interactor.getChartInfo(coinCode, currency.code, chartType)
        interactor.observeChartInfo(coinCode, currency.code, chartType)

    }

    override fun onTouchSelect(point: ChartPoint) {
        val currencyValue = CurrencyValue(currency, point.value.toBigDecimal())
        view.showSelectedPoint(Triple(point.timestamp, currencyValue, chartType))
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

    private fun updateChart() {
        val cInfo = chartInfo ?: return
        val mInfo = marketInfo ?: return

        view.hideSpinner()

        try {
            val viewItem = factory.createViewItem(chartType, cInfo, mInfo, currency)
            view.showChart(viewItem)
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

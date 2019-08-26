package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class RateChartPresenter(
        val view: View,
        private val interactor: Interactor,
        private val coinCode: CoinCode,
        private val currency: Currency,
        private val factory: RateChartViewFactory)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    private var lastRate: Rate? = null
    private var rateStat: RateStatData? = null
    private var chartType = interactor.defaultChartType

    //  ViewDelegate

    override fun viewDidLoad() {
        view.showSpinner()
        view.setChartType(interactor.defaultChartType)
        showOrFetch()
    }

    override fun onSelect(type: ChartType) {
        chartType = type
        interactor.defaultChartType = type

        view.showSpinner()
        showOrFetch()
    }

    override fun onTouchSelect(point: DataPoint) {
        view.showSelectedPoint(Pair(point.time, CurrencyValue(currency, point.value.toBigDecimal())))
    }

    //  InteractorDelegate

    override fun onReceiveStats(data: Pair<RateStatData, Rate>) {
        rateStat = data.first
        lastRate = data.second

        enableButtons(data.first.stats)
        showStats()
    }

    override fun onReceiveError(ex: Throwable) {
        view.hideSpinner()
        view.showError(ex)
    }

    private fun showOrFetch() {
        if (rateStat == null) {
            interactor.getRateStats(coinCode, currency.code)
        } else {
            showStats()
        }
    }

    private fun showStats() {
        view.hideSpinner()

        try {
            val viewItem = factory.createViewItem(chartType, rateStat, lastRate, currency)
            view.showChart(viewItem)
        } catch (e: Exception) {
            view.showError(e)
        }
    }

    private fun enableButtons(rates: Map<String, RateData>) {
        val chartTypes = ChartType.values().associateBy(ChartType::name)

        rates.forEach { (key, data) ->
            if (data.rates.size > 10) {
                chartTypes[key]?.let { view.enableChartType(it) }
            }
        }
    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

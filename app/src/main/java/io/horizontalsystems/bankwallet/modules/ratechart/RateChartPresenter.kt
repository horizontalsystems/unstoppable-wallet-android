package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
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

    private var rate: Rate? = null
    private var statsData: StatsData? = null
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

    override fun onReceiveStats(data: Pair<StatsData, Rate>) {
        rate = data.second
        statsData = data.first
        val stats = data.first.stats

        for (type in stats.keys) {
            val chartType = ChartType.fromString(type) ?: continue
            val chartData = stats[chartType.name] ?: continue
            if (chartData.points.size > 10) {
                view.enableChartType(chartType)
            }
        }

        showChart()
    }

    override fun onReceiveError(ex: Throwable) {
        view.hideSpinner()
        view.showError(ex)
    }

    private fun showOrFetch() {
        if (statsData == null) {
            interactor.getRateStats(coinCode, currency.code)
        } else {
            showChart()
        }
    }

    private fun showChart() {
        val statsData = statsData ?: return

        view.hideSpinner()

        try {
            val viewItem = factory.createViewItem(chartType, statsData, rate, currency)
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

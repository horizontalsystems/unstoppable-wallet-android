package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class RateChartPresenter(val view: View, private val interactor: Interactor, private val coinCode: CoinCode)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    private var lastRate: Rate? = null
    private var rateStats: RateStatData? = null

    //  ViewDelegate

    override val currency: Currency
        get() = interactor.chartCurrency

    override fun viewDidLoad() {
        view.showSpinner()
        view.setDefaultMode(interactor.defaultChartMode)
        showOrFetch(interactor.defaultChartMode)
    }

    override fun onClick(mode: Mode) {
        view.showSpinner()
        showOrFetch(mode)
    }

    //  InteractorDelegate

    override fun setDefault(mode: Mode) {
        view.setDefaultMode(mode)
    }

    override fun showChart(data: Pair<RateStatData, Rate>, mode: Mode) {
        rateStats = data.first
        lastRate = data.second

        showStats(mode)
    }

    private fun showOrFetch(mode: Mode) {
        if (rateStats == null) {
            interactor.getData(coinCode, mode)
        } else {
            showStats(mode)
        }
    }

    private fun showStats(mode: Mode) {
        val stat = rateStats ?: return
        val rate = lastRate ?: return
        val data = stat.stats[mode.name] ?: return

        val fst = data.rates[0].toBigDecimal()
        val max = data.rates.max() ?: 0f
        val min = data.rates.min() ?: 0f

        val rates = when(mode) {
            Mode.MONTHLY18 -> data.rates.takeLast(52) // for one year
            else -> data.rates
        }

        view.showRate(rate.value, fst)
        view.showMarketCap(stat.marketCap, max.toBigDecimal(), min.toBigDecimal())
        view.showChart(ChartData(rates, data.timestamp, data.scale, mode))
        view.hideSpinner()
    }

    override fun showError(error: Throwable) {

    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

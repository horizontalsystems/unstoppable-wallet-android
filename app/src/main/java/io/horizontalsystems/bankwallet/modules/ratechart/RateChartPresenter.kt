package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.Interactor
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.InteractorDelegate
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.View
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartModule.ViewDelegate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

class RateChartPresenter(val view: View, private val interactor: Interactor, private val coinCode: CoinCode)
    : ViewModel(), ViewDelegate, InteractorDelegate {

    //  ViewDelegate

    override val currency: Currency
        get() = interactor.getCurrency()

    override fun viewDidLoad() {
        view.showSpinner()
        interactor.getData(coinCode)
    }

    override fun onClick(mode: Mode) {
        view.showSpinner()
        interactor.getData(coinCode, mode)
    }

    //  InteractorDelegate

    override fun setDefault(mode: Mode) {
        view.setDefaultMode(mode)
    }

    override fun showRate(rate: BigDecimal, startRate: BigDecimal) {
        view.showRate(rate, startRate)
    }

    override fun showMarketCap(value: BigDecimal, high: BigDecimal, low: BigDecimal) {
        view.showMarketCap(value, high, low)
    }

    override fun showChart(data: ChartData) {
        view.showChart(data)
        view.hideSpinner()
    }

    override fun showError(error: Throwable) {

    }

    //  ViewModel

    override fun onCleared() {
        interactor.clear()
    }
}

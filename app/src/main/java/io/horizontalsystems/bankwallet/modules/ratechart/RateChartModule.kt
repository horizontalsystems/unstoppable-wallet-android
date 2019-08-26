package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

object RateChartModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun setChartType(type: ChartType)
        fun enableChartType(type: ChartType)
        fun showChart(viewItem: ChartViewItem)
        fun showSelectedPoint(data: Pair<Long, CurrencyValue>)
        fun showError(ex: Throwable)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(type: ChartType)
        fun onTouchSelect(point: DataPoint)
    }

    interface Interactor {
        var defaultChartType: ChartType
        fun getRateStats(coinCode: CoinCode, currencyCode: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun onReceiveStats(data: Pair<RateStatData, Rate>)
        fun onReceiveError(ex: Throwable)
    }

    interface Router

    class Factory(private val coin: Coin) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RateChartView()
            val interactor = RateChartInteractor(App.rateManager, App.rateStorage, App.localStorage)
            val presenter = RateChartPresenter(view, interactor, coin.code, App.currencyManager.baseCurrency, RateChartViewFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

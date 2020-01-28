package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo

object RateChartModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun setChartType(type: ChartType)
        fun showChartInfo(viewItem: ChartInfoViewItem)
        fun showMarketInfo(viewItem: MarketInfoViewItem)
        fun showSelectedPoint(data: Triple<Long, CurrencyValue, ChartType>)
        fun showError(ex: Throwable)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(type: ChartType)
        fun onTouchSelect(point: ChartPoint)
    }

    interface Interactor {
        var defaultChartType: ChartType?

        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun getChartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo?
        fun observeChartInfo(coinCode: String, currencyCode: String, chartType: ChartType)
        fun observeMarketInfo(coinCode: String, currencyCode: String)
        fun clear()
    }

    interface InteractorDelegate {
        fun onUpdate(chartInfo: ChartInfo)
        fun onUpdate(marketInfo: MarketInfo)
        fun onError(ex: Throwable)
    }

    interface Router

    class Factory(private val coin: Coin) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)

            val view = RateChartView()
            val interactor = RateChartInteractor(App.xRateManager, App.localStorage)
            val presenter = RateChartPresenter(view, rateFormatter, interactor, coin, currency, RateChartViewFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

object RateChartModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun setChartType(type: ChartType)
        fun showChartInfo(viewItem: ChartInfoViewItem)
        fun showMarketInfo(viewItem: MarketInfoViewItem)
        fun showSelectedPointInfo(item: ChartPointViewItem)
        fun showError(ex: Throwable)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onSelect(type: ChartType)
        fun onTouchSelect(point: PointInfo, macdIsVisible: Boolean)
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

    class Factory(private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val currency = App.currencyManager.baseCurrency
            val rateFormatter = RateFormatter(currency)

            val view = RateChartView()
            val interactor = RateChartInteractor(App.xRateManager, App.chartTypeStorage)
            val presenter = RateChartPresenter(view, rateFormatter, interactor, coinCode, currency, RateChartViewFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }

    data class CoinCodeWithValue(val coinCode: String, val value: BigDecimal)

}

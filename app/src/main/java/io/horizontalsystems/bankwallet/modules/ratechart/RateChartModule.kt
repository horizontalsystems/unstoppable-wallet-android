package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

object RateChartModule {

    interface View {
        fun showSpinner()
        fun hideSpinner()
        fun setDefaultMode(mode: Mode)
        fun showRate(rate: BigDecimal, startRate: BigDecimal)
        fun showMarketCap(value: BigDecimal, high: BigDecimal, low: BigDecimal)
        fun showChart(data: ChartData)
    }

    interface ViewDelegate {
        val currency: Currency
        fun viewDidLoad()
        fun onClick(mode: Mode)
    }

    interface Interactor {
        fun getCurrency(): Currency
        fun getData(coinCode: CoinCode, mode: Mode? = null)
        fun clear()
    }

    interface InteractorDelegate {
        fun setDefault(mode: Mode)
        fun showRate(rate: BigDecimal, startRate: BigDecimal)
        fun showMarketCap(value: BigDecimal, high: BigDecimal, low: BigDecimal)
        fun showChart(data: ChartData)
        fun showError(error: Throwable)
    }

    interface Router

    class Factory(private val coin: Coin) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RateChartView()
            val interactor = RateChartInteractor(App.currencyManager.baseCurrency, App.rateManager, App.rateStorage, App.localStorage)
            val presenter = RateChartPresenter(view, interactor, coin.code)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

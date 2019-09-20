package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.CurrentDateProvider
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import java.math.BigDecimal
import java.util.*

object RateListModule {

    interface IView {
        fun showCurrentDate(currentDate: Date)
        fun reload()
    }

    interface IRouter {
    }

    interface IViewDelegate {
        fun viewDidLoad()
        val itemsCount: Int
        fun getViewItem(position: Int): RateViewItem
    }

    interface IInteractor {
        val currentDate: Date

        fun clear()
        fun initRateList()
        fun getRateStats(coinCodes: List<String>, currencyCode: String)
        fun fetchRates(coinCodes: List<String>, currencyCode: String)
    }

    interface IInteractorDelegate{
        fun onReceive(statsData: StatsData)
        fun onFailStats(coinCode: String)
        fun didUpdateRate(rate: Rate)
        fun willEnterForeground()
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            var coins = App.walletStorage.enabledCoins()
            if (coins.isEmpty()) {
                coins = App.appConfigProvider.featuredCoins
            }
            val view = RateListView()
            val interactor = RatesInteractor(App.rateStatsManager, App.rateStorage, CurrentDateProvider(), App.backgroundManager)
            val presenter = RateListPresenter(view, interactor, DataSource(App.currencyManager.baseCurrency, coins))

            interactor.delegate = presenter

            return presenter as T
        }
    }

    class DataSource(val baseCurrency: Currency, coins: List<Coin>) {

        private val chartType = ChartView.ChartType.DAILY.name

        var items = coins.map {coin ->
            val viewItem = RateViewItem(coin)
            viewItem
        }

        val coinCodes: List<String>
            get() {
                return items.map { it.coin.code }
            }

        fun setChartData(statsData: StatsData) {
            val chartDiff = statsData.diff[chartType]
            getPositionByCoinCode(statsData.coinCode)?.let { position ->
                items[position].diff = chartDiff
                items[position].loadingStatus = RateLoadingStatus.Loaded
            }
        }

        fun setRate(rate: Rate) {
            getPositionByCoinCode(rate.coinCode)?.let { position ->
                items[position].rate = CurrencyValue(baseCurrency, rate.value)
                items[position].rateExpired = rate.expired
            }
        }

        fun setStatsFailed(coinCode: String) {
            getPositionByCoinCode(coinCode)?.let {position ->
                items[position].loadingStatus = RateLoadingStatus.Failed
            }
        }

        private fun getPositionByCoinCode(coinCode: String): Int? {
            val index = items.indexOfFirst { it.coin.code == coinCode }
            return if (index >= 0) index else null
        }
    }
}

class RateViewItem(val coin: Coin) {
    var rateExpired: Boolean = false
    var rate: CurrencyValue? = null
    var diff: BigDecimal? = null
    var loadingStatus: RateLoadingStatus = RateLoadingStatus.Loading
}

enum class RateLoadingStatus{
    Loading, Loaded, Failed
}

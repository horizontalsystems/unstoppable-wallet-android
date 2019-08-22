package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class RateChartInteractor(
        private val currency: Currency,
        private val rateManager: RateManager,
        private val rateStorage: IRateStorage,
        private val localStorage: ILocalStorage)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private val disposable = CompositeDisposable()

    override fun getCurrency(): Currency {
        return currency
    }

    override fun getData(coinCode: CoinCode, mode: Mode?) {
        val chartMode = getChartMode(mode)

        val getRates = rateManager.getRateStats(coinCode, currency.code, chartMode.name.toLowerCase())
        val getLocalRate = rateStorage.latestRateObservable(coinCode, currency.code)

        Flowable.zip(getRates, getLocalRate, BiFunction<RateStatData, Rate, Pair<RateStatData, Rate>> { a, b -> Pair(a, b) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onFetch(it, chartMode) }, this::onFetchError)
                .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }

    private fun onFetch(data: Pair<RateStatData, Rate>, mode: Mode) {
        val (stats, rate) = data

        val fst = stats.rates[0].toBigDecimal()
        val max = stats.rates.max() ?: 0f
        val min = stats.rates.min() ?: 0f

        localStorage.chartMode = mode

        delegate?.showRate(rate.value, fst)
        delegate?.showMarketCap(rate.value.pow(5), max.toBigDecimal(), min.toBigDecimal())
        delegate?.showChart(ChartData(stats.rates, stats.timestamp, stats.scale, mode))
    }

    private fun onFetchError(error: Throwable) {
        delegate?.showError(error)
    }

    private fun getChartMode(mode: Mode?): Mode {
        var chartMode = mode
        if (chartMode == null) {
            chartMode = localStorage.chartMode
            delegate?.setDefault(chartMode)
        }

        return chartMode
    }
}

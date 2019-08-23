package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
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

    override val chartCurrency: Currency get() = currency

    override var defaultChartMode: Mode
        get() = localStorage.chartMode
        set(value) {
            localStorage.chartMode = value
        }

    override fun getData(coinCode: CoinCode, mode: Mode) {
        val getRates = rateManager.getRateStats(coinCode, currency.code)
        val getLocalRate = rateStorage.latestRateObservable(coinCode, currency.code)

        Flowable.zip(getRates, getLocalRate, BiFunction<RateStatData, Rate, Pair<RateStatData, Rate>> { a, b -> Pair(a, b) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onFetch(it, mode) }, this::onFetchError)
                .let { disposable.add(it) }
    }

    override fun clear() {
        disposable.clear()
    }

    private fun onFetch(data: Pair<RateStatData, Rate>, mode: Mode) {
        defaultChartMode = mode
        delegate?.showChart(data, mode)
    }

    private fun onFetchError(error: Throwable) {
        delegate?.showError(error)
    }
}

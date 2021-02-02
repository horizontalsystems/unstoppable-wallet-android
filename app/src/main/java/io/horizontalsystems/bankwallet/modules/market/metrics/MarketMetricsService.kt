package io.horizontalsystems.bankwallet.modules.market.metrics

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketMetricsService(
        private val xRateManager: IRateManager,
        val currency: Currency
) : Clearable {

    val marketMetricsObservable: BehaviorSubject<DataState<GlobalCoinMarket>> = BehaviorSubject.createDefault(DataState.Loading)

    private val disposables = CompositeDisposable()

    init {
        fetchMarketMetrics()
    }

    fun refresh() {
        fetchMarketMetrics()
    }

    private fun fetchMarketMetrics() {
        marketMetricsObservable.onNext(DataState.Loading)

        xRateManager.getGlobalMarketInfoAsync(currency.code)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    marketMetricsObservable.onNext(DataState.Success(it))
                }, {
                    marketMetricsObservable.onNext(DataState.Error(it))
                })
                .let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }
}

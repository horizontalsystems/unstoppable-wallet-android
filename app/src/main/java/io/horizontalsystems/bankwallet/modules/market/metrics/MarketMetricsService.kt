package io.horizontalsystems.bankwallet.modules.market.metrics

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MarketMetricsService(
        private val xRateManager: IRateManager,
        private val currency: Currency
) : Clearable {

    val marketMetricsObservable: BehaviorSubject<DataState<MarketMetricsItem>> = BehaviorSubject.createDefault(DataState.Loading)

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
                .subscribeIO({
                    val marketMetricsItem = MarketMetricsItem.createFromGlobalCoinMarket(it, currency)
                    marketMetricsObservable.onNext(DataState.Success(marketMetricsItem))
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

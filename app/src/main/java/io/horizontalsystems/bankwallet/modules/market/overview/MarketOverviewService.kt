package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketOverviewService(
    private val topMarketsRepository: TopMarketsRepository,
    private val marketMetricsRepository: MarketMetricsRepository,
    private val backgroundManager: BackgroundManager,
    private val currencyManager: ICurrencyManager
) : BackgroundManager.Listener {

    private val topListSize = 5
    private var currencyManagerDisposable: Disposable? = null
    private var gainersDisposable: Disposable? = null
    private var losersDisposable: Disposable? = null
    private var metricsDisposable: Disposable? = null

    var gainersTopMarket: TopMarket = TopMarket.Top250
        private set
    var losersTopMarket: TopMarket = TopMarket.Top250
        private set

    val topMarketOptions: List<TopMarket> = TopMarket.values().toList()
    val topGainersObservable: BehaviorSubject<Result<List<MarketItem>>> = BehaviorSubject.create()
    val topLosersObservable: BehaviorSubject<Result<List<MarketItem>>> = BehaviorSubject.create()
    val marketMetricsObservable: BehaviorSubject<Result<MarketMetricsItem>> =
        BehaviorSubject.create()

    private fun updateGainers(forceRefresh: Boolean) {
        gainersDisposable?.dispose()

        topMarketsRepository
            .get(
                gainersTopMarket.value,
                SortingField.TopGainers,
                topListSize,
                currencyManager.baseCurrency,
                forceRefresh
            )
            .subscribeIO(
                { topGainersObservable.onNext(Result.success(it)) },
                { topGainersObservable.onNext(Result.failure(it)) }
            )
            .let { gainersDisposable = it }
    }

    private fun updateLosers(forceRefresh: Boolean) {
        losersDisposable?.dispose()

        topMarketsRepository
            .get(
                losersTopMarket.value,
                SortingField.TopLosers,
                topListSize,
                currencyManager.baseCurrency,
                forceRefresh
            )
            .subscribeIO(
                { topLosersObservable.onNext(Result.success(it)) },
                { topLosersObservable.onNext(Result.failure(it)) }
            )
            .let { losersDisposable = it }
    }

    private fun updateMarketMetrics() {
        metricsDisposable?.dispose()

        marketMetricsRepository.get(currencyManager.baseCurrency, true)
            .subscribeIO(
                { marketMetricsObservable.onNext(Result.success(it)) },
                { marketMetricsObservable.onNext(Result.failure(it)) }
            )
            .let { metricsDisposable = it }
    }

    private fun forceRefresh() {
        updateGainers(true)
        updateLosers(true)
        updateMarketMetrics()
    }

    fun start() {
        backgroundManager.registerListener(this)
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { forceRefresh() }
            .let { currencyManagerDisposable = it }

        forceRefresh()
    }

    fun stop() {
        currencyManagerDisposable?.dispose()
        gainersDisposable?.dispose()
        losersDisposable?.dispose()
        metricsDisposable?.dispose()
        backgroundManager.unregisterListener(this)
    }

    override fun willEnterForeground() {
        forceRefresh()
    }

    fun refresh() {
        forceRefresh()
    }

    fun setGainersTopMarket(topMarket: TopMarket) {
        gainersTopMarket = topMarket
        updateGainers(false)
    }

    fun setLosersTopMarket(topMarket: TopMarket) {
        losersTopMarket = topMarket
        updateLosers(false)
    }
}

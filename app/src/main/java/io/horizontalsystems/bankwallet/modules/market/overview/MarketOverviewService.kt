package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
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

    val topGainersObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val topLosersObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val marketMetricsObservable: BehaviorSubject<DataState<MarketMetricsItem>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private fun updateGainers(forceRefresh: Boolean) {
        gainersDisposable?.dispose()

        topMarketsRepository.get(
            gainersTopMarket.value,
            SortingField.TopGainers,
            topListSize,
            currencyManager.baseCurrency,
            forceRefresh
        )
            .doOnSubscribe { topGainersObservable.onNext(DataState.Loading) }
            .subscribeIO(
                { topGainersObservable.onNext(DataState.Success(it)) },
                { topGainersObservable.onNext(DataState.Error(it)) }
            )
            .let { gainersDisposable = it }
    }

    private fun updateLosers(forceRefresh: Boolean) {
        losersDisposable?.dispose()

        topMarketsRepository.get(
            losersTopMarket.value,
            SortingField.TopLosers,
            topListSize,
            currencyManager.baseCurrency,
            forceRefresh
        )
            .doOnSubscribe { topLosersObservable.onNext(DataState.Loading) }
            .subscribeIO(
                { topLosersObservable.onNext(DataState.Success(it)) },
                { topLosersObservable.onNext(DataState.Error(it)) }
            )
            .let { losersDisposable = it }
    }

    private fun updateMarketMetrics() {
        metricsDisposable?.dispose()

        marketMetricsRepository.get(currencyManager.baseCurrency, true)
            .doOnSubscribe { marketMetricsObservable.onNext(DataState.Loading) }
            .subscribeIO(
                { marketMetricsObservable.onNext(DataState.Success(it)) },
                { marketMetricsObservable.onNext(DataState.Error(it)) }
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

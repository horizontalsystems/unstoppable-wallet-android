package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketOverviewService(
    private val topMarketsRepository: TopMarketsRepository,
    private val marketMetricsRepository: MarketMetricsRepository,
    private val backgroundManager: BackgroundManager,
    private val currencyManager: ICurrencyManager
) : BackgroundManager.Listener {

    private val topListSize = 5

    var gainersTopMarket: TopMarket = TopMarket.Top250
        private set
    var losersTopMarket: TopMarket = TopMarket.Top250
        private set

    private val disposables = CompositeDisposable()
    private var currencyManagerDisposable: Disposable? = null

    val topGainersObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val topLosersObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val marketMetricsObservable: BehaviorSubject<DataState<MarketMetricsItem>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private fun fetch(forceRefresh: Boolean = true) {
        topMarketsRepository.get(
            gainersTopMarket,
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
            .let { disposables.add(it) }

        topMarketsRepository.get(
            losersTopMarket,
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
            .let { disposables.add(it) }

        marketMetricsRepository.get(currencyManager.baseCurrency, forceRefresh)
            .doOnSubscribe { marketMetricsObservable.onNext(DataState.Loading) }
            .subscribeIO(
                { marketMetricsObservable.onNext(DataState.Success(it)) },
                { marketMetricsObservable.onNext(DataState.Error(it)) }
            )
            .let { disposables.add(it) }
    }

    fun start() {
        backgroundManager.registerListener(this)
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { fetch() }
            .let { currencyManagerDisposable = it }

        fetch()
    }

    fun stop() {
        currencyManagerDisposable?.dispose()
        disposables.dispose()
        backgroundManager.unregisterListener(this)
    }

    override fun willEnterForeground() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun onToggleTopGainersBoard() {
        gainersTopMarket = gainersTopMarket.next()
        fetch(forceRefresh = false)
    }

    fun onToggleTopLosersBoard() {
        losersTopMarket = losersTopMarket.next()
        fetch(forceRefresh = false)
    }

}

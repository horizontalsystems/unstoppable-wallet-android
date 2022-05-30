package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.DiscoveryItem.Category
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsRepository
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsRepository
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformItem
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import io.horizontalsystems.bankwallet.modules.nft.TopNftCollection
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.*

class MarketOverviewService(
    private val marketTopCoinsRepository: MarketTopCoinsRepository,
    private val marketMetricsRepository: MarketMetricsRepository,
    private val topNftCollectionsRepository: TopNftCollectionsRepository,
    private val topSectorsRepository: TopSectorsRepository,
    private val topPlatformsRepository: TopPlatformsRepository,
    private val backgroundManager: BackgroundManager,
    private val currencyManager: ICurrencyManager
) : BackgroundManager.Listener {

    private val topListSize = 5
    private var currencyManagerDisposable: Disposable? = null
    private var gainersDisposable: Disposable? = null
    private var losersDisposable: Disposable? = null
    private var metricsDisposable: Disposable? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var topNftsJob: Job? = null
    private var topSectorsJob: Job? = null
    private var topPlatformsJob: Job? = null

    var gainersTopMarket: TopMarket = TopMarket.Top250
        private set
    var losersTopMarket: TopMarket = TopMarket.Top250
        private set
    var topNftsTimeDuration: TimeDuration = TimeDuration.SevenDay
        private set
    val topNftsSortingField: SortingField = SortingField.HighestVolume
    var topPlatformsTimeDuration: TimeDuration = TimeDuration.OneDay
        private set
    private val topPlatformsSortingField: SortingField = SortingField.HighestVolume

    val topMarketOptions: List<TopMarket> = TopMarket.values().toList()
    val timeDurationOptions: List<TimeDuration> = TimeDuration.values().toList()
    val topGainersObservable: BehaviorSubject<Result<List<MarketItem>>> = BehaviorSubject.create()
    val topLosersObservable: BehaviorSubject<Result<List<MarketItem>>> = BehaviorSubject.create()
    val marketMetricsObservable: BehaviorSubject<Result<MarketMetricsItem>> = BehaviorSubject.create()
    val topNftCollectionsObservable: BehaviorSubject<Result<List<TopNftCollection>>> = BehaviorSubject.create()
    val topSectorsObservable: BehaviorSubject<Result<List<Category>>> = BehaviorSubject.create()
    val topPlatformsObservable: BehaviorSubject<Result<List<TopPlatformItem>>> = BehaviorSubject.create()

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private fun updateGainers(forceRefresh: Boolean) {
        gainersDisposable?.dispose()

        marketTopCoinsRepository
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

        marketTopCoinsRepository
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

    private fun updateTopSectors() {
        topSectorsJob?.cancel()

        topSectorsJob = coroutineScope.launch {
            try {
                val topSectors = topSectorsRepository.get(currencyManager.baseCurrency, true)
                topSectorsObservable.onNext(Result.success(topSectors))
            } catch (cancellation: CancellationException) {
                // do nothing
            } catch (error: Exception) {
                topSectorsObservable.onNext(Result.failure(error))
            }
        }
    }

    private fun updateTopNftCollections(forceRefresh: Boolean) {
        topNftsJob?.cancel()

        topNftsJob = coroutineScope.launch {
            try {
                val topNftCollections = topNftCollectionsRepository.get(
                    sortingField = topNftsSortingField,
                    timeDuration = topNftsTimeDuration,
                    forceRefresh = forceRefresh,
                    limit = topListSize
                )
                topNftCollectionsObservable.onNext(Result.success(topNftCollections))
            } catch (cancellation: CancellationException) {
                // do nothing
            } catch (error: Exception) {
                topNftCollectionsObservable.onNext(Result.failure(error))
            }
        }
    }

    private fun updateTopPlatforms(forceRefresh: Boolean) {
        topPlatformsJob?.cancel()

        topPlatformsJob = coroutineScope.launch {
            try {
                val topPlatforms = topPlatformsRepository.get(
                    sortingField = topPlatformsSortingField,
                    timeDuration = topPlatformsTimeDuration,
                    forceRefresh = forceRefresh,
                    limit = topListSize
                )
                topPlatformsObservable.onNext(Result.success(topPlatforms))
            } catch (cancellation: CancellationException) {
                // do nothing
            } catch (error: Exception) {
                topPlatformsObservable.onNext(Result.failure(error))
            }
        }
    }

    private fun forceRefresh() {
        updateGainers(true)
        updateLosers(true)
        updateMarketMetrics()
        updateTopNftCollections(true)
        updateTopSectors()
        updateTopPlatforms(true)
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
        coroutineScope.cancel()
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

    fun setTopNftsTimeDuration(timeDuration: TimeDuration) {
        topNftsTimeDuration = timeDuration
        updateTopNftCollections(false)
    }

    fun setTopPlatformsTimeDuration(timeDuration: TimeDuration) {
        topPlatformsTimeDuration = timeDuration
        updateTopPlatforms(false)
    }

}

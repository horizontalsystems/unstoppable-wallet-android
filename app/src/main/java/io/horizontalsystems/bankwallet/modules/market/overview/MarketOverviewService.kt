package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopMoversRepository
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.MarketOverview
import io.horizontalsystems.marketkit.models.TopMovers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class MarketOverviewService(
    private val marketTopMoversRepository: MarketTopMoversRepository,
    private val marketKit: MarketKitWrapper,
    private val backgroundManager: BackgroundManager,
    private val currencyManager: CurrencyManager
) : BackgroundManager.Listener {

    private var currencyManagerDisposable: Disposable? = null
    private var topMoversDisposable: Disposable? = null
    private var marketOverviewDisposable: Disposable? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val topMarketOptions: List<TopMarket> = TopMarket.values().toList()
    val timeDurationOptions: List<TimeDuration> = listOf(
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )
    val topMoversObservable: BehaviorSubject<Result<TopMovers>> = BehaviorSubject.create()
    val marketOverviewObservable: BehaviorSubject<Result<MarketOverview>> = BehaviorSubject.create()

    private fun updateTopMovers() {
        topMoversDisposable?.dispose()

        marketTopMoversRepository
            .getTopMovers(currencyManager.baseCurrency)
            .subscribeIO(
                { topMoversObservable.onNext(Result.success(it)) },
                { topMoversObservable.onNext(Result.failure(it)) }
            )
            .let { topMoversDisposable = it }
    }

    private fun updateMarketOverview() {
        marketOverviewDisposable?.dispose()

        marketKit.marketOverviewSingle(currencyManager.baseCurrency.code)
            .subscribeIO(
                { marketOverviewObservable.onNext(Result.success(it)) },
                { marketOverviewObservable.onNext(Result.failure(it)) }
            )
            .let { marketOverviewDisposable = it }
    }

    private fun forceRefresh() {
        updateTopMovers()
        updateMarketOverview()
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
        topMoversDisposable?.dispose()
        marketOverviewDisposable?.dispose()
        backgroundManager.unregisterListener(this)
        coroutineScope.cancel()
    }

    override fun willEnterForeground() {
        forceRefresh()
    }

    fun refresh() {
        forceRefresh()
    }

}

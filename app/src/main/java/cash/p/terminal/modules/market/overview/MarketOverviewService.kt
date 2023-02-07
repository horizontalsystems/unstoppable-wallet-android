package cash.p.terminal.modules.market.overview

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.topcoins.MarketTopMoversRepository
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
    val timeDurationOptions: List<TimeDuration> = TimeDuration.values().toList()
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

package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketOverviewService(
    private val marketKit: MarketKit,
    private val xRateManager: IRateManager,
    private val backgroundManager: BackgroundManager,
    private val currencyManager: ICurrencyManager
) : Clearable, BackgroundManager.Listener {

    data class MarketOverviewItem(
        val marketMetrics: MarketMetricsItem,
        val marketItems: List<MarketItem>
    )

    val stateObservable: BehaviorSubject<DataState<Unit>> =
        BehaviorSubject.createDefault(DataState.Loading)

    var marketOverviewItem: MarketOverviewItem? = null

    private val disposables = CompositeDisposable()
    private var marketOverviewDisposable: Disposable? = null

    init {
        fetch()
        backgroundManager.registerListener(this)
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                fetch()
            }
            .let {
                disposables.add(it)
            }
    }

    override fun willEnterForeground() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        marketOverviewDisposable?.dispose()

        stateObservable.onNext(DataState.Loading)

        marketOverviewDisposable = Single.zip(
            xRateManager.getGlobalMarketInfoAsync(currencyManager.baseCurrency.code),
            marketKit.marketInfosSingle(1000, 1000, null),
            { t1, t2 -> Pair(t1, t2) }
        ).subscribeIO({ (globalCoinMarket, marketInfos) ->
            val marketMetrics =
                MarketMetricsItem.createFromGlobalCoinMarket(globalCoinMarket, currencyManager.baseCurrency)
            val marketItems = marketInfos.mapIndexed { index, marketInfo ->
                MarketItem.createFromCoinMarket(marketInfo, currencyManager.baseCurrency, Score.Rank(index + 1))
            }
            marketOverviewItem = MarketOverviewItem(marketMetrics, marketItems)
            stateObservable.onNext(DataState.Success(Unit))
        }, {
            stateObservable.onNext(DataState.Error(it))
        })
    }

    override fun clear() {
        marketOverviewDisposable?.dispose()
        disposables.dispose()
        backgroundManager.unregisterListener(this)
    }

}

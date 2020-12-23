package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MarketTopService(
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager
) : Clearable {

    val periods: Array<Period> = Period.values()
    var period: Period = Period.Period24h
        set(value) {
            field = value

            syncTopItemsByPeriod()
        }

    val marketTopItemsObservable: BehaviorSubject<DataState<List<MarketTopItem>>> = BehaviorSubject.createDefault(DataState.Loading)
    val currency by currencyManager::baseCurrency

    private var fullItems: List<TopMarket> = listOf()
    private var marketTopItems: List<MarketTopItem> = listOf()

    private val disposable = CompositeDisposable()

    init {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        marketTopItemsObservable.onNext(DataState.Loading)

        xRateManager.getTopMarketList(currencyManager.baseCurrency.code)
                .subscribe({
                    fullItems = it
                    syncTopItemsByPeriod()
                }, {
                    marketTopItemsObservable.onNext(DataState.Error(it))
                })
                .let {
                    disposable.add(it)
                }
    }

    private fun syncTopItemsByPeriod() {
        marketTopItems = fullItems.map {
            MarketTopItem(
                    it.coinCode,
                    it.coinName,
                    it.marketInfo.marketCap,
                    it.marketInfo.volume,
                    it.marketInfo.rate,
                    it.marketInfo.diff,
            )
        }

        marketTopItemsObservable.onNext(DataState.Success(marketTopItems))
    }

    override fun clear() {
        disposable.clear()
    }

}

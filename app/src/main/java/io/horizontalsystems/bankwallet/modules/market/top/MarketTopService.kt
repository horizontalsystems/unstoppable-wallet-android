package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
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

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }


    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)
    val currency by currencyManager::baseCurrency

    private var fullItems: List<TopMarket> = listOf()
    var marketTopItems: List<MarketTopItem> = listOf()

    private val disposable = CompositeDisposable()

    init {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        stateObservable.onNext(State.Loading)

        xRateManager.getTopMarketList(currencyManager.baseCurrency.code)
                .subscribe({
                    fullItems = it
                    syncTopItemsByPeriod()
                }, {
                    stateObservable.onNext(State.Error(it))
                })
                .let {
                    disposable.add(it)
                }
    }

    private fun syncTopItemsByPeriod() {
        var i = 1
        marketTopItems = fullItems.map {
            MarketTopItem(
                    i++,
                    it.coinCode,
                    it.coinName,
                    it.marketInfo.marketCap,
                    it.marketInfo.volume,
                    it.marketInfo.rate,
                    it.marketInfo.diff,
            )
        }

        stateObservable.onNext(State.Loaded)
    }

    override fun clear() {
        disposable.clear()
    }

}

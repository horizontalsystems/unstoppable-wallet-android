package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketTopService(
        private val currencyManager: ICurrencyManager,
        private val marketListDataSource: IMarketListDataSource
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

        marketListDataSource.dataUpdatedAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    fetch()
                }
                .let {
                    disposable.add(it)
                }
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        stateObservable.onNext(State.Loading)

        marketListDataSource.getListAsync(currencyManager.baseCurrency.code)
                .subscribeOn(Schedulers.io())
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
                    it.coin.code,
                    it.coin.title,
                    it.marketInfo.marketCap.toDouble(),
                    it.marketInfo.volume.toDouble(),
                    it.marketInfo.rate,
                    it.marketInfo.rateDiff,
            )
        }

        stateObservable.onNext(State.Loaded)
    }

    override fun clear() {
        disposable.clear()
    }

}

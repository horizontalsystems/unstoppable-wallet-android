package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketOverviewService(
        private val currencyManager: ICurrencyManager,
        private val rateManager: IRateManager
) : Clearable {

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)
    val currency by currencyManager::baseCurrency

    var marketItems: List<MarketItem> = listOf()

    private var topItemsDisposable: Disposable? = null
    private val disposable = CompositeDisposable()

    init {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    private fun fetch() {
        topItemsDisposable?.let { disposable.remove(it) }

        stateObservable.onNext(State.Loading)

        topItemsDisposable = rateManager.getTopMarketList(currencyManager.baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    marketItems = it.mapIndexed { index, topMarket ->
                        MarketItem.createFromCoinMarket(topMarket, Score.Rank(index + 1))
                    }

                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Error(it))
                })

        topItemsDisposable?.let {
            disposable.add(it)
        }
    }

    override fun clear() {
        disposable.clear()
    }

}

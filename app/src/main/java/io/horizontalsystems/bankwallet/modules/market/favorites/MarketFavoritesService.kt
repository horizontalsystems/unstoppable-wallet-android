package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketFavoritesService(
        val currency: Currency,
        private val rateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : Clearable {

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    var marketItems: List<MarketItem> = listOf()

    private var topItemsDisposable: Disposable? = null
    private val disposable = CompositeDisposable()

    init {
        fetch()

        marketFavoritesManager.dataUpdatedAsync
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
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
        topItemsDisposable?.let { disposable.remove(it) }

        stateObservable.onNext(State.Loading)

        val coinCodes = marketFavoritesManager.getAll().map { it.code }

        topItemsDisposable = rateManager.getCoinMarketList(coinCodes, currency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    marketItems = it.mapIndexed { index, topMarket ->
                        MarketItem.createFromCoinMarket(topMarket, null)
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

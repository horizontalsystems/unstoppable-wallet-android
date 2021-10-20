package io.horizontalsystems.bankwallet.modules.market.list

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketListService(
        private val fetcher: IMarketListFetcher,
        private val currencyManager: ICurrencyManager
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

        Observable.merge(fetcher.dataUpdatedAsync, currencyManager.baseCurrencyUpdatedSignal)
                .subscribeIO {
                    marketItems = listOf()
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

        topItemsDisposable = fetcher.fetchAsync(currencyManager.baseCurrency)
                .subscribeIO({
                    marketItems = it
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

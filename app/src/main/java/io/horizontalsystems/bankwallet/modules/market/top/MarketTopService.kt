package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.core.ICurrencyManager
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

            fetch()
        }

    val sortingFields: Array<Field> = marketListDataSource.sortingFields
    var sortingField: Field = Field.HighestCap
        set(value) {
            field = value

            fetch()
        }


    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }


    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)
    val currency by currencyManager::baseCurrency

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

        marketListDataSource.getListAsync(currencyManager.baseCurrency.code, period, sortingField)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    marketTopItems = it

                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Error(it))
                })
                .let {
                    disposable.add(it)
                }
    }

    override fun clear() {
        disposable.clear()
    }

}

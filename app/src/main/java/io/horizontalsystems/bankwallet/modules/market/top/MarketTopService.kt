package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MarketTopService(
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager
) : Clearable {

    val sortingFields: Array<Field> = Field.values()
    val sortingPeriods: Array<Period> = Period.values()

    var sortingField: Field = Field.HighestCap
        set(value) {
            field = value

            sortingFieldObservable.onNext(value)
        }

    var sortingPeriod: Period = Period.Period24h
        set(value) {
            field = value

            sortingPeriodObservable.onNext(value)
        }

    val sortingFieldObservable = BehaviorSubject.createDefault(sortingField)
    val sortingPeriodObservable = BehaviorSubject.createDefault(sortingPeriod)
    val marketTopItemsObservable: BehaviorSubject<DataState<List<MarketTopItem>>> = BehaviorSubject.createDefault(DataState.Loading)
    val currency by currencyManager::baseCurrency

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
                    marketTopItemsObservable.onNext(DataState.Success(
                            it.map {
                                MarketTopItem(
                                        it.coinCode,
                                        it.coinName,
                                        it.marketInfo.marketCap,
                                        it.marketInfo.volume,
                                        it.marketInfo.rate,
                                        it.marketInfo.diff,
                                )
                            }

                    ))
                }, {
                    marketTopItemsObservable.onNext(DataState.Error(it))
                })
                .let {
                    disposable.add(it)
                }
    }

    override fun clear() {
        disposable.clear()
    }

}

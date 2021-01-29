package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketTopService(
        private val currencyManager: ICurrencyManager,
        private val marketListDataSource: IMarketListDataSource,
        private val rateManager: IRateManager
) : Clearable {

    val sortingFields: Array<SortingField> = marketListDataSource.sortingFields

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)
    val currency by currencyManager::baseCurrency

    var marketTopItems: List<MarketTopItem> = listOf()

    private var topItemsDisposable: Disposable? = null
    private val disposable = CompositeDisposable()

    init {
        fetch()

        marketListDataSource.dataUpdatedAsync
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

        topItemsDisposable = marketListDataSource.getListAsync(currencyManager.baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    marketTopItems = it.mapIndexed { index, topMarket ->
                        convertToMarketTopItem(index + 1, topMarket)
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

    private fun convertToMarketTopItem(rank: Int, topMarket: CoinMarket) =
            MarketTopItem(
                    Score.Rank(rank),
                    topMarket.coin.code,
                    topMarket.coin.title,
                    topMarket.marketInfo.volume,
                    topMarket.marketInfo.rate,
                    topMarket.marketInfo.rateDiffPeriod,
                    topMarket.marketInfo.marketCap
            )
}

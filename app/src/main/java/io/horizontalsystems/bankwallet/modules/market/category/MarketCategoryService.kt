package io.horizontalsystems.bankwallet.modules.market.category

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketCategoryService(
    private val marketCategoryRepository: MarketCategoryRepository,
    private val currencyManager: ICurrencyManager,
    private val coinCategoryUid: String,
    topMarket: TopMarket = TopMarket.Top250,
    sortingField: SortingField = SortingField.HighestCap,
) {
    private var disposable: Disposable? = null

    val stateObservable: BehaviorSubject<DataState<List<MarketItem>>> = BehaviorSubject.createDefault(DataState.Loading)

    var topMarket: TopMarket = topMarket
        private set

    val sortingFields = SortingField.values().toList()
    var sortingField: SortingField = sortingField
        private set

    fun setSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync(false)
    }

    private fun sync(forceRefresh: Boolean) {
        disposable?.dispose()

        marketCategoryRepository.get(
            coinCategoryUid,
            topMarket.value,
            sortingField,
            topMarket.value,
            currencyManager.baseCurrency,
            forceRefresh
        )
            .doOnSubscribe { stateObservable.onNext(DataState.Loading) }
            .subscribeIO({
                stateObservable.onNext(DataState.Success(it))
            }, {
                stateObservable.onNext(DataState.Error(it))
            }).let {
                disposable = it
            }
    }

    fun start() {
        sync(true)
    }

    fun refresh() {
        sync(true)
    }

    fun stop() {
        disposable?.dispose()
    }
}

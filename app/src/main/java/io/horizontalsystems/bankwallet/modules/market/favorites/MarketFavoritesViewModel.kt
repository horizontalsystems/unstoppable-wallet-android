package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MarketFavoritesViewModel(
        private val service: MarketFavoritesService,
        private val connectivityManager: ConnectivityManager,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<SortingField> = SortingField.values()

    var sortingField: SortingField = sortingFields.first()
        private set

    var marketField: MarketField = MarketField.PriceDiff
        private set

    fun update(sortingField: SortingField? = null, marketField: MarketField? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        marketField?.let {
            this.marketField = it
        }
        syncViewItemsBySortingField()
    }

    val marketViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val networkNotAvailable = SingleLiveEvent<Unit>()
    val showEmptyListTextLiveData = MutableLiveData(false)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketFavoritesService.State) {
        loadingLiveData.postValue(state is MarketFavoritesService.State.Loading)

        if (state is MarketFavoritesService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketFavoritesService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketFavoritesService.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun syncViewItemsBySortingField() {
        val viewItems = service.marketItems
                .sort(sortingField)
                .map {
                    MarketViewItem.create(it, service.currency.symbol, marketField)
                }

        showEmptyListTextLiveData.postValue(viewItems.isEmpty())

        marketViewItemsLiveData.postValue(viewItems)
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }


    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
        super.onCleared()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

}

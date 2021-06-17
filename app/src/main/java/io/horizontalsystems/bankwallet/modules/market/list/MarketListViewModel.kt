package io.horizontalsystems.bankwallet.modules.market.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class MarketListViewModel(
        private val service: MarketListService,
        private val connectivityManager: ConnectivityManager,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<SortingField> = SortingField.values()

    var sortingField: SortingField = sortingFields.first()
        private set

    private var marketFieldIndex: Int = MarketField.values().indexOf(MarketField.PriceDiff)

    val marketFields: List<MarketListHeaderView.FieldViewOption> = MarketField.values().mapIndexed { index, marketField ->
        MarketListHeaderView.FieldViewOption(index, Translator.getString(marketField.titleResId), index == marketFieldIndex)
    }

    fun update(sortingField: SortingField? = null, marketFieldIndex: Int? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        marketFieldIndex?.let {
            this.marketFieldIndex = it
        }
        syncViewItemsBySortingField(sortingField != null)
    }

    val marketViewItemsLiveData = MutableLiveData<Pair<List<MarketViewItem>,Boolean>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val networkNotAvailable = SingleLiveEvent<Unit>()
    val showEmptyListTextLiveData = MutableLiveData(false)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeIO {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketListService.State) {
        loadingLiveData.postValue(state is MarketListService.State.Loading)

        if (state is MarketListService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketListService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketListService.State.Loaded) {
            syncViewItemsBySortingField(false)
        }
    }

    private fun syncViewItemsBySortingField(scrollToTop: Boolean) {
        val viewItems = service.marketItems
                .sort(sortingField)
                .map { marketItem ->
                    MarketViewItem.create(marketItem, MarketField.values()[marketFieldIndex])
                }

        showEmptyListTextLiveData.postValue(viewItems.isEmpty())

        marketViewItemsLiveData.postValue(Pair(viewItems, scrollToTop))
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }


    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

}

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
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class MarketListViewModel(
    private val service: MarketListService,
    private val connectivityManager: ConnectivityManager,
    private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<SortingField> = SortingField.values()

    val marketViewItemsLiveData = MutableLiveData<Pair<List<MarketViewItem>, Boolean>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val networkNotAvailable = SingleLiveEvent<Unit>()
    val showEmptyListTextLiveData = MutableLiveData(false)
    val topMenuLiveData = MutableLiveData(Pair(getSortMenu(), getToggleButton()))

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

    private fun getToggleButton(): MarketListHeaderView.ToggleButton {
        return MarketListHeaderView.ToggleButton(
            title = Translator.getString(service.marketField.titleResId),
            indicators = MarketField.values()
                .mapIndexed { index, _ -> ToggleIndicator(index == service.marketField.ordinal) }
        )
    }

    private fun getSortMenu(): MarketListHeaderView.SortMenu {
        return MarketListHeaderView.SortMenu.MultiOption(Translator.getString(service.sortingField.titleResId))
    }

    private fun syncState(state: MarketListService.State) {
        loadingLiveData.postValue(state is MarketListService.State.Loading)

        if (state is MarketListService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketListService.State.Error)?.error?.let {
            convertErrorMessage(it)
        })

        if (state is MarketListService.State.Loaded) {
            syncViewItemsBySortingField(false)
        }
    }

    private fun syncViewItemsBySortingField(scrollToTop: Boolean) {
        val viewItems = service.marketItems
            .sort(service.sortingField)
            .map { MarketViewItem.create(it, service.marketField) }

        showEmptyListTextLiveData.postValue(viewItems.isEmpty())

        marketViewItemsLiveData.postValue(Pair(viewItems, scrollToTop))
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    private fun updateTopMenu() {
        topMenuLiveData.postValue(Pair(getSortMenu(), getToggleButton()))
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun updateSorting(sortingField: SortingField) {
        service.sortingField = sortingField
        syncViewItemsBySortingField(true)
        updateTopMenu()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun onToggleButtonClick() {
        service.marketField = service.marketField.next()
        syncViewItemsBySortingField(false)
        updateTopMenu()
    }

    fun getSortingMenuItems(): List<SelectorItem> {
        return sortingFields.map {
            SelectorItem(Translator.getString(it.titleResId), it == service.sortingField)
        }
    }
}

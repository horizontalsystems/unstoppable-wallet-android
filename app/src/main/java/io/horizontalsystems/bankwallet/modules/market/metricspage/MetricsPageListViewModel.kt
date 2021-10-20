package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.list.MarketListService
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageListService.State
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class MetricsPageListViewModel(
    private val service: MetricsPageListService,
    private val connectivityManager: ConnectivityManager,
    private val clearables: List<Clearable>
) : ViewModel() {

    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val marketViewItemsLiveData = MutableLiveData<Pair<List<MarketViewItem>,Boolean>>()
    val networkNotAvailable = SingleLiveEvent<Unit>()

    private var sortDesc = true
    private var marketFieldIndex: Int = MarketField.values().indexOf(MarketField.PriceDiff)
    private val disposables = CompositeDisposable()

    private val sortMenu: MarketListHeaderView.SortMenu
        get() {
            val direction =
                if (sortDesc) MarketListHeaderView.Direction.Down else MarketListHeaderView.Direction.Up
            return MarketListHeaderView.SortMenu.DuoOption(direction)
        }

    val listHeaderMenu = MutableLiveData(MetricsPageListHeaderAdapter.ViewItemWrapper(sortMenu, getToggleButton()))

    init {
        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    fun onChangeSorting() {
        sortDesc = !sortDesc
        syncViewItemsBySortingField(true)
        updateTopMenu()
    }

    fun onErrorClick() {
        service.refresh()
    }

    private fun syncState(state: State) {
        loadingLiveData.postValue(state is State.Loading)

        if (state is State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketListService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is State.Loaded) {
            syncViewItemsBySortingField(false)
        }
    }

    private fun syncViewItemsBySortingField(scrollToTop: Boolean) {
        val sortingField = if (sortDesc) SortingField.HighestCap else SortingField.LowestCap
        val viewItems = service.marketItems
            .sort(sortingField)
            .mapNotNull { marketItem ->
                MarketField.fromIndex(marketFieldIndex)?.let { marketField ->
                    MarketViewItem.create(marketItem, marketField)
                }
            }

        marketViewItemsLiveData.postValue(Pair(viewItems, scrollToTop))
    }

    private fun updateTopMenu() {
        listHeaderMenu.postValue(
            MetricsPageListHeaderAdapter.ViewItemWrapper(sortMenu, getToggleButton())
        )
    }

    fun onToggleButtonClick() {
        var newIndex = marketFieldIndex + 1
        if (newIndex >= MarketField.values().size){
            newIndex = 0
        }
        marketFieldIndex = newIndex
        syncViewItemsBySortingField(false)
        updateTopMenu()
    }

    private fun getToggleButton(): MarketListHeaderView.ToggleButton {
        val marketFields: List<String> =
            MarketField.values().map { Translator.getString(it.titleResId) }

        return MarketListHeaderView.ToggleButton(
            title = marketFields[marketFieldIndex],
            indicators = marketFields.mapIndexed { index, _ -> ToggleIndicator(index == marketFieldIndex) }
        )
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }
}

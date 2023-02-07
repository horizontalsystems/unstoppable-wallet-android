package cash.p.terminal.modules.market.topnftcollections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.viewState
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TopNftCollectionsViewModel(
    private val service: TopNftCollectionsService,
    private val topNftCollectionsViewItemFactory: TopNftCollectionsViewItemFactory
) : ViewModel() {

    val header = MarketModule.Header(
        Translator.getString(R.string.Nft_TopCollections),
        Translator.getString(R.string.Nft_TopCollections_Description),
        ImageSource.Local(R.drawable.ic_top_nfts)
    )

    val sortingField by service::sortingField
    val timeDuration by service::timeDuration

    var menu by mutableStateOf(
        Menu(
            sortingFieldSelect = Select(service.sortingField, service.sortingFields),
            timeDurationSelect = Select(service.timeDuration, service.timeDurations)
        )
    )
        private set

    var viewItems by mutableStateOf<List<TopNftCollectionViewItem>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var sortingFieldSelectDialog by mutableStateOf<Select<SortingField>?>(null)
        private set

    init {
        service.topNftCollections.collectWith(viewModelScope) { result ->
            result.viewState?.let {
                viewState = it
            }

            result.getOrNull()?.let { topNftCollections ->
                viewItems = topNftCollections.mapIndexed { index, collection ->
                    topNftCollectionsViewItemFactory.viewItem(collection, service.timeDuration, index + 1)
                }
            }
        }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun updateMenu() {
        menu = Menu(
            sortingFieldSelect = Select(service.sortingField, service.sortingFields),
            timeDurationSelect = Select(service.timeDuration, service.timeDurations)
        )
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            service.refresh()

            isRefreshing = true
            delay(1000)
            isRefreshing = false
        }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        viewModelScope.launch {
            service.setSortingField(sortingField)
        }

        updateMenu()

        sortingFieldSelectDialog = null
    }

    fun onSelectTimeDuration(timeDuration: TimeDuration) {
        viewModelScope.launch {
            service.setTimeDuration(timeDuration)
        }

        updateMenu()
    }

    fun onSelectorDialogDismiss() {
        sortingFieldSelectDialog = null
    }

    fun onClickSortingFieldMenu() {
        sortingFieldSelectDialog = Select(service.sortingField, service.sortingFields)
    }

}

package io.horizontalsystems.bankwallet.modules.market.platform

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketPlatformViewModel(
    private val repository: MarketPlatformCoinsRepository,
    private val favoritesManager: MarketFavoritesManager,
) : ViewModelUiState<MarketPlatformUiState>() {

    val sortingFields = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers,
    )

    private var sortingField: SortingField = SortingField.HighestCap
    private var viewState: ViewState = ViewState.Loading
    private var viewItems: List<MarketViewItem> = listOf()
    private var cache: List<MarketItem> = emptyList()
    private var isRefreshing = false

    init {
        sync()
    }

    override fun createState() = MarketPlatformUiState(
        viewItems = viewItems,
        viewState = viewState,
        sortingField = sortingField,
        isRefreshing = isRefreshing,
    )

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()
    }

    fun onAddFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
        sync()
    }

    fun onRemoveFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
        sync()
    }

    private fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!forceRefresh && cache.isNotEmpty()) {
                viewItems = cache
                    .sort(sortingField)
                    .map { item ->
                        marketViewItem(item)
                    }
                viewState = ViewState.Success
                emitState()
            } else {
                fetchFromRepository(forceRefresh)
            }
        }
    }

    private suspend fun fetchFromRepository(forceRefresh: Boolean) {
        try {
            viewItems = repository.get(sortingField, forceRefresh)?.map {
                marketViewItem(it)
            } ?: listOf()

            viewState = ViewState.Success
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
        emitState()
    }

    private fun marketViewItem(item: MarketItem): MarketViewItem = MarketViewItem.create(
        marketItem = item,
        favorited = favoritesManager.getAll().map { it.coinUid }.contains(item.fullCoin.coin.uid)
    )

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            sync(true)

            isRefreshing = true
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }
}

data class MarketPlatformUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val isRefreshing: Boolean,
)
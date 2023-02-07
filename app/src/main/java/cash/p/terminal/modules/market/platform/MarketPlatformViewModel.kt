package cash.p.terminal.modules.market.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.iconUrl
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.*
import cash.p.terminal.modules.market.topcoins.SelectorDialogState
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketPlatformViewModel(
    platform: Platform,
    private val repository: MarketPlatformCoinsRepository,
    private val favoritesManager: MarketFavoritesManager,
) : ViewModel() {

    private val sortingFields = SortingField.values().toList()

    private val marketFields = MarketField.values().toList()

    var sortingField: SortingField = SortingField.HighestCap
        private set

    var marketField: MarketField = MarketField.PriceDiff
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var viewItems by mutableStateOf<List<MarketViewItem>>(listOf())
        private set

    val header = MarketModule.Header(
        Translator.getString(R.string.MarketPlatformCoins_PlatformEcosystem, platform.name),
        Translator.getString(
            R.string.MarketPlatformCoins_PlatformEcosystemDescription,
            platform.name
        ),
        ImageSource.Remote(platform.iconUrl)
    )

    var isRefreshing by mutableStateOf(false)
        private set

    var selectorDialogState by mutableStateOf<SelectorDialogState>(SelectorDialogState.Closed)
        private set

    var menu by mutableStateOf(
        MarketPlatformModule.Menu(
            sortingFieldSelect = Select(sortingField, sortingFields),
            marketFieldSelect = Select(marketField, marketFields)
        )
    )
        private set

    init {
        sync()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        selectorDialogState = SelectorDialogState.Closed
        sync()
        updateMenu()
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField
        sync()
        updateMenu()
    }

    fun onSelectorDialogDismiss() {
        selectorDialogState = SelectorDialogState.Closed
    }

    fun showSelectorMenu() {
        selectorDialogState = SelectorDialogState.Opened(
            Select(sortingField, sortingFields)
        )
    }

    fun onAddFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
        sync()
    }

    fun onRemoveFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
        sync()
    }

    private fun updateMenu() {
        menu = MarketPlatformModule.Menu(
            sortingFieldSelect = Select(sortingField, sortingFields),
            marketFieldSelect = Select(marketField, marketFields)
        )
    }

    private fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val marketItems = repository.get(sortingField, forceRefresh)
                val favorites = favoritesManager.getAll().map { it.coinUid }
                viewItems = marketItems?.map {
                    MarketViewItem.create(
                        it,
                        marketField,
                        favorites.contains(it.fullCoin.coin.uid)
                    )
                } ?: listOf()
                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
        }
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            sync(true)

            isRefreshing = true
            delay(1000)
            isRefreshing = false
        }
    }
}

package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.favorites.period
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlin.enums.EnumEntries
import kotlin.math.min

class MarketTopCoinsViewModel(
    private var topMarket: TopMarket,
    private var sortingField: SortingField,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val favoritesManager: MarketFavoritesManager,
) : ViewModelUiState<MarketTopCoinsUiState>() {

    private val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )
    private val sortingFields = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers,
    )
    private val topMarkets = TopMarket.entries
    private val baseCurrency get() = currencyManager.baseCurrency

    private var isRefreshing = false
    private var viewState: ViewState = ViewState.Loading
    private var viewItems: List<MarketViewItem> = listOf()
    private var period = periods[0]
    private var favoriteCoinUids: List<String> = listOf()

    override fun createState() = MarketTopCoinsUiState(
        isRefreshing = isRefreshing,
        viewState = viewState,
        viewItems = viewItems,
        topMarkets = topMarkets,
        topMarket = topMarket,
        sortingFields = sortingFields,
        sortingField = sortingField,
        periods = periods,
        period = period,
    )

    private var marketInfoList: List<MarketInfo>? = null
    private var marketItemList: List<MarketItem>? = null
    private var sortedMarketItems: List<MarketItem>? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                reload()

                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }

            emitState()
        }

        viewModelScope.launch(Dispatchers.Default) {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                refreshFavoriteCoinUids()
                refreshViewItems()

                emitState()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                try {
                    reload()

                    viewState = ViewState.Success
                } catch (e: Throwable) {
                    viewState = ViewState.Error(e)
                }

                emitState()
            }
        }
    }

    private suspend fun reload() {
        fetchMarketInfoList()

        refreshFavoriteCoinUids()
        refreshMarketItemList()
        refreshSortedMarketItems()
        refreshViewItems()
    }

    private fun refreshFavoriteCoinUids() {
        favoriteCoinUids = favoritesManager.getAll().map { it.coinUid }
    }

    private suspend fun fetchMarketInfoList() {
        marketInfoList = marketKit.topCoinsMarketInfosSingle(500, baseCurrency.code).await()
    }

    private fun refreshMarketItemList() {
        marketItemList = marketInfoList?.map { marketInfo ->
            MarketItem.createFromCoinMarket(
                marketInfo,
                baseCurrency,
                period.period,
            )
        }
    }

    private fun refreshSortedMarketItems() {
        sortedMarketItems = marketItemList?.let { list ->
            list
                .subList(0, min(list.size, topMarket.value))
                .sort(sortingField)
        }
    }

    private fun refreshViewItems() {
        sortedMarketItems?.let { list ->
            viewItems = list.map {
                MarketViewItem.create(it, favoriteCoinUids.contains(it.fullCoin.coin.uid))
            }
        }
    }

    fun refresh() {
        isRefreshing = true
        emitState()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                reload()

                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }

            isRefreshing = false
            emitState()
        }
    }

    fun onAddFavorite(uid: String) {
        viewModelScope.launch(Dispatchers.Default) {
            favoritesManager.add(uid)
        }
    }

    fun onRemoveFavorite(uid: String) {
        viewModelScope.launch(Dispatchers.Default) {
            favoritesManager.remove(uid)
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        emitState()

        viewModelScope.launch(Dispatchers.Default) {
            refreshSortedMarketItems()
            refreshViewItems()

            emitState()
        }
    }

    fun onSelectTopMarket(topMarket: TopMarket) {
        this.topMarket = topMarket
        emitState()

        viewModelScope.launch(Dispatchers.Default) {
            refreshSortedMarketItems()
            refreshViewItems()

            emitState()
        }
    }

    fun onSelectPeriod(period: TimeDuration) {
        this.period = period
        emitState()

        viewModelScope.launch(Dispatchers.Default) {
            refreshMarketItemList()
            refreshSortedMarketItems()
            refreshViewItems()

            emitState()
        }
    }

    class Factory(
        private val topMarket: TopMarket,
        private val sortingField: SortingField,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketTopCoinsViewModel(
                topMarket,
                sortingField,
                App.marketKit,
                App.currencyManager,
                App.marketFavoritesManager
            ) as T
        }
    }
}

data class MarketTopCoinsUiState(
    val isRefreshing: Boolean,
    val viewState: ViewState,
    val viewItems: List<MarketViewItem>,
    val topMarkets: EnumEntries<TopMarket>,
    val topMarket: TopMarket,
    val sortingFields: List<SortingField>,
    val sortingField: SortingField,
    val periods: List<TimeDuration>,
    val period: TimeDuration,
)

package com.quantum.wallet.bankwallet.modules.market.sector

import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.managers.LanguageManager
import com.quantum.wallet.bankwallet.core.managers.MarketFavoritesManager
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statSortType
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.market.MarketItem
import com.quantum.wallet.bankwallet.modules.market.MarketViewItem
import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.modules.market.TimeDuration
import com.quantum.wallet.bankwallet.modules.market.TopMarket
import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MarketSectorViewModel(
    private val marketCategoryRepository: MarketSectorRepository,
    private val currencyManager: CurrencyManager,
    private val languageManager: LanguageManager,
    private val favoritesManager: MarketFavoritesManager,
    private val coinCategory: CoinCategory,
    private val topMarket: TopMarket,
) : ViewModelUiState<MarketSectorUiState>() {

    val sortingOptions = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers
    )

    val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )

    val categoryName = coinCategory.name
    val categoryDescription: String
        get() = coinCategory.description[languageManager.currentLocaleTag]
            ?: coinCategory.description["en"]
            ?: coinCategory.description.keys.firstOrNull()
            ?: ""

    private var syncJob: Job? = null
    private var viewItems = emptyList<MarketViewItem>()
    private var sortingField = SortingField.TopGainers
    private var timePeriod = periods.first()
    private var marketItems = emptyList<MarketItem>()
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing = false

    init {
        viewModelScope.launch {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                viewItems = getMarketViewItems(marketItems)
                emitState()
            }
        }

        sync()
    }

    override fun createState(): MarketSectorUiState {
        return MarketSectorUiState(
            sortingField = sortingField,
            timePeriod = timePeriod,
            viewItems = viewItems,
            viewState = viewState,
            isRefreshing = isRefreshing
        )
    }

    private fun sync(forceRefresh: Boolean = false) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                marketItems = marketCategoryRepository
                    .get(
                        coinCategory.uid,
                        topMarket.value,
                        sortingField,
                        timePeriod,
                        topMarket.value,
                        currencyManager.baseCurrency,
                        forceRefresh
                    )
                    .await()
                viewItems = getMarketViewItems(marketItems)
                viewState = ViewState.Success
                emitState()
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
                emitState()
            }
        }
    }

    private fun getMarketViewItems(marketItems: List<MarketItem>): List<MarketViewItem> {
        val favorites = favoritesManager.getAll().map { it.coinUid }
        return marketItems.map {
            MarketViewItem.create(
                marketItem = it,
                favorited = favorites.contains(it.fullCoin.coin.uid)
            )
        }
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            isRefreshing = true
            emitState()

            sync(true)

            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()

        stat(
            page = StatPage.CoinCategory,
            event = StatEvent.SwitchSortType(sortingField.statSortType)
        )
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onAddFavorite(uid: String) {
        favoritesManager.add(uid)

        stat(page = StatPage.CoinCategory, event = StatEvent.AddToWatchlist(uid))
    }

    fun onRemoveFavorite(uid: String) {
        favoritesManager.remove(uid)

        stat(page = StatPage.CoinCategory, event = StatEvent.RemoveFromWatchlist(uid))
    }

    fun onTimePeriodSelect(timePeriod: TimeDuration) {
        this.timePeriod = timePeriod
        sync()
    }
}

data class MarketSectorUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val timePeriod: TimeDuration,
    val isRefreshing: Boolean,
)

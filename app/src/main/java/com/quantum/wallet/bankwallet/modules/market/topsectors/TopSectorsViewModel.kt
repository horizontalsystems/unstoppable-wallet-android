package com.quantum.wallet.bankwallet.modules.market.topsectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.IAppNumberFormatter
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.market.MarketDataValue
import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.modules.market.TimeDuration
import com.quantum.wallet.bankwallet.modules.market.Value
import com.quantum.wallet.bankwallet.modules.market.sortedByDescendingNullLast
import com.quantum.wallet.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TopSectorsViewModel(
    private val currencyManager: CurrencyManager,
    private val topSectorsRepository: TopSectorsRepository,
    private val numberFormatter: IAppNumberFormatter
) : ViewModelUiState<TopSectorsUiState>() {
    private var isRefreshing = false
    private var items = listOf<TopSectorViewItem>()
    private var viewState: ViewState = ViewState.Loading

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
    )

    private var sortingField = SortingField.TopGainers

    private var timePeriod = periods.first()

    init {
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                fetchItems()
                emitState()
            }
        }

        viewModelScope.launch {
            fetchItems()
            emitState()
        }
    }

    override fun createState() = TopSectorsUiState(
        isRefreshing = isRefreshing,
        items = items,
        viewState = viewState,
        sortingField = sortingField,
        timePeriod = timePeriod,
    )

    private suspend fun fetchItems(forceRefresh: Boolean = false) =
        withContext(Dispatchers.Default) {
            try {
                val topSectors =
                    topSectorsRepository.get(currencyManager.baseCurrency, forceRefresh)

                val topCategoryWithDiffList = topSectors.map {
                    TopSectorWithDiff(
                        it.coinCategory,
                        changeValue(it.coinCategory, timePeriod),
                        it.topCoins
                    )
                }
                val sortedTopSectors = topCategoryWithDiffList.sort(sortingField)
                items = sortedTopSectors.map { getViewItem(it) }
                viewState = ViewState.Success
            } catch (e: CancellationException) {
                // no-op
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }

    private fun getViewItem(item: TopSectorWithDiff) =
        TopSectorViewItem(
            coinCategory = item.coinCategory,
            marketCapValue = item.coinCategory.marketCap?.let {
                numberFormatter.formatFiatShort(
                    it,
                    currencyManager.baseCurrency.symbol,
                    2
                )
            },
            changeValue = item.diff?.let {
                MarketDataValue.Diff(Value.Percent(it))
            },
            coin1 = item.topCoins[0],
            coin2 = item.topCoins[1],
            coin3 = item.topCoins[2],
        )

    private fun changeValue(category: CoinCategory, timePeriod: TimeDuration): BigDecimal? {
        return when (timePeriod) {
            TimeDuration.OneDay -> category.diff24H
            TimeDuration.SevenDay -> category.diff1W
            TimeDuration.ThirtyDay -> category.diff1M
            else -> null
        }
    }

    private fun List<TopSectorWithDiff>.sort(sortingField: SortingField) = when (sortingField) {
        SortingField.HighestCap -> sortedByDescendingNullLast { it.coinCategory.marketCap }
        SortingField.LowestCap -> sortedByNullLast { it.coinCategory.marketCap }
        SortingField.TopGainers -> sortedByDescendingNullLast { it.diff }
        SortingField.TopLosers -> sortedByNullLast { it.diff }
        else -> this
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            emitState()

            fetchItems()
            delay(1000)
            isRefreshing = false
            emitState()
        }

        stat(
            page = StatPage.Markets,
            event = StatEvent.Refresh,
            section = StatSection.Pairs
        )
    }

    fun onErrorClick() {
        refresh()
    }

    fun onTimePeriodSelect(timePeriod: TimeDuration) {
        this.timePeriod = timePeriod

        viewModelScope.launch {
            fetchItems()
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField

        viewModelScope.launch {
            fetchItems()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopSectorsViewModel(
                App.currencyManager,
                TopSectorsRepository(App.marketKit),
                App.numberFormatter
            ) as T
        }
    }
}

data class TopSectorWithDiff(
    val coinCategory: CoinCategory,
    val diff: BigDecimal?,
    val topCoins: List<FullCoin>
)

data class TopSectorsUiState(
    val isRefreshing: Boolean,
    val items: List<TopSectorViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val timePeriod: TimeDuration,
)

data class TopSectorViewItem(
    val coinCategory: CoinCategory,
    val marketCapValue: String?,
    val changeValue: MarketDataValue?,
    val coin1: FullCoin,
    val coin2: FullCoin,
    val coin3: FullCoin,
)
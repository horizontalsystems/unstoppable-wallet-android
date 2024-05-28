package io.horizontalsystems.bankwallet.modules.market.etf

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.etf.EtfModule.EtfViewItem
import io.horizontalsystems.marketkit.models.Etf
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class EtfViewModel(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) : ViewModelUiState<EtfModule.UiState>() {

    val timeDurations = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )
    val sortByOptions = listOf(
        EtfModule.SortBy.HighestAssets,
        EtfModule.SortBy.LowestAssets,
        EtfModule.SortBy.Inflow,
        EtfModule.SortBy.Outflow
    )
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing: Boolean = false
    private var viewItems: List<EtfViewItem> = listOf()
    private var marketDataJob: Job? = null
    private var sortBy: EtfModule.SortBy = sortByOptions.first()
    private var timeDuration: TimeDuration = timeDurations.first()
    private var cachedEtfs: List<Etf> = listOf()

    override fun createState(): EtfModule.UiState {
        return EtfModule.UiState(
            viewItems = viewItems,
            viewState = viewState,
            isRefreshing = isRefreshing,
            timeDuration = timeDuration,
            sortBy = sortBy,
        )
    }

    init {
        syncItems()
    }

    private fun syncItems() {
        if (cachedEtfs.isEmpty()) {
            fetchEtfs()
        } else {
            updateViewItems()
            emitState()
        }
    }

    private fun fetchEtfs() {
        marketDataJob?.cancel()
        marketDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                cachedEtfs = marketKit.etfs(currencyManager.baseCurrency.code).await()
                updateViewItems()

                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun updateViewItems() {
        val sorted = when (sortBy) {
            EtfModule.SortBy.HighestAssets -> cachedEtfs.sortedByDescending { it.totalAssets }
            EtfModule.SortBy.LowestAssets -> cachedEtfs.sortedBy { it.totalAssets }
            EtfModule.SortBy.Inflow -> cachedEtfs.sortedByDescending {
                it.priceChangeValue(
                    timeDuration
                )
            }

            EtfModule.SortBy.Outflow -> cachedEtfs.sortedBy { it.priceChangeValue(timeDuration) }
        }
        viewItems = sorted.map { etf ->
            etfViewItem(etf, timeDuration)
        }
    }

    private fun etfViewItem(etf: Etf, timeDuration: TimeDuration) = EtfViewItem(
        title = etf.ticker,
        iconUrl = "https://cdn.blocksdecoded.com/header-images/${etf.ticker.lowercase()}@3x.png",
        subtitle = etf.name,
        value = etf.totalAssets?.let {
            App.numberFormatter.formatFiatShort(it, currencyManager.baseCurrency.symbol, 0)
        },
        subvalue = etf.priceChangeValue(timeDuration)?.let {
            MarketDataValue.DiffNew(
                Value.Currency(
                    CurrencyValue(currencyManager.baseCurrency, it)
                )
            )
        },
        rank = null
    )

    private fun refreshWithMinLoadingSpinnerPeriod() {
        isRefreshing = true
        emitState()
        syncItems()
        viewModelScope.launch {
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectTimeDuration(selected: TimeDuration) {
        timeDuration = selected
        syncItems()
    }

    fun onSelectSortBy(selected: EtfModule.SortBy) {
        sortBy = selected
        syncItems()
    }

}

private fun Etf.priceChangeValue(timeDuration: TimeDuration): BigDecimal? {
    return when (timeDuration) {
        TimeDuration.OneDay -> inflows[HsTimePeriod.Day1]
        TimeDuration.SevenDay -> inflows[HsTimePeriod.Week1]
        TimeDuration.ThirtyDay -> inflows[HsTimePeriod.Month1]
        TimeDuration.ThreeMonths -> inflows[HsTimePeriod.Month3]
    }
}

package io.horizontalsystems.bankwallet.modules.market.toppairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.StatSortType
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TopPair
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TopPairsViewModel(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) : ViewModelUiState<TopPairsUiState>() {
    private var isRefreshing = false
    private var items = listOf<TopPairViewItem>()
    private var viewState: ViewState = ViewState.Loading
    private var sortDescending = true

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

    override fun createState() = TopPairsUiState(
        isRefreshing = isRefreshing,
        items = items,
        viewState = viewState,
        sortDescending = sortDescending,
    )

    private fun sortItems(items: List<TopPairViewItem>) =
        if (sortDescending) items.sortedByDescending { it.volume } else items.sortedBy { it.volume }

    private suspend fun fetchItems() = withContext(Dispatchers.Default) {
        try {
            val topPairs =
                marketKit.topPairsSingle(currencyManager.baseCurrency.code, 1, 100).await()
            val pairs = topPairs.map {
                TopPairViewItem.createFromTopPair(it, currencyManager.baseCurrency.symbol)
            }
            items = sortItems(pairs)
            viewState = ViewState.Success
        } catch (e: CancellationException) {
            // no-op
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
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
            section = StatSection.Pairs,
            event = StatEvent.Refresh
        )

    }

    fun onErrorClick() {
        refresh()
    }

    fun toggleSorting() {
        sortDescending = !sortDescending
        emitState()
        viewModelScope.launch {
            items = sortItems(items)
            emitState()
        }

        stat(
            page = StatPage.Markets,
            section = StatSection.Pairs,
            event = StatEvent.SwitchSortType(if (sortDescending) StatSortType.HighestVolume else StatSortType.LowestVolume)
        )
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopPairsViewModel(App.marketKit, App.currencyManager) as T
        }
    }

}

data class TopPairsUiState(
    val isRefreshing: Boolean,
    val items: List<TopPairViewItem>,
    val viewState: ViewState,
    val sortDescending: Boolean,
)

data class TopPairViewItem(
    val title: String,
    val rank: String,
    val name: String,
    val base: String,
    val baseCoinUid: String?,
    val baseCoin: Coin?,
    val target: String,
    val targetCoinUid: String?,
    val targetCoin: Coin?,
    val iconUrl: String?,
    val tradeUrl: String?,
    val volume: BigDecimal,
    val volumeInFiat: String,
    val price: String?
) {
    companion object {
        fun createFromTopPair(topPair: TopPair, currencySymbol: String): TopPairViewItem {
            val volumeStr = App.numberFormatter.formatFiatShort(topPair.volume, currencySymbol, 2)

            val priceStr = topPair.price?.let {
                App.numberFormatter.formatCoinShort(
                    it,
                    topPair.target,
                    8
                )
            }

            return TopPairViewItem(
                title = "${topPair.base}/${topPair.target}",
                name = topPair.marketName ?: "",
                base = topPair.base,
                baseCoinUid = topPair.baseCoinUid,
                baseCoin = topPair.baseCoin,
                target = topPair.target,
                targetCoinUid = topPair.targetCoinUid,
                targetCoin = topPair.targetCoin,
                iconUrl = topPair.marketLogo,
                rank = topPair.rank.toString(),
                tradeUrl = topPair.tradeUrl,
                volume = topPair.volume,
                volumeInFiat = volumeStr,
                price = priceStr,
            )
        }
    }
}
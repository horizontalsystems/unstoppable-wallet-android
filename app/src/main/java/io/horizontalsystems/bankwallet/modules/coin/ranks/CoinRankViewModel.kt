package cash.p.terminal.modules.coin.ranks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.IAppNumberFormatter
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.RankType
import cash.p.terminal.modules.coin.ranks.CoinRankModule.RankAnyValue
import cash.p.terminal.modules.coin.ranks.CoinRankModule.UiState
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.Select
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.RankMultiValue
import io.horizontalsystems.marketkit.models.RankValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class CoinRankViewModel(
    private val rankType: RankType,
    private val baseCurrency: Currency,
    private val marketKit: MarketKitWrapper,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private var internalItems: List<InternalItem> = emptyList()
    private var viewState: ViewState = ViewState.Loading
    private val periodOptions = TimeDuration.values().toList()
    private var selectedPeriod: TimeDuration = periodOptions[2]
    private val showPeriodMenu = rankType != RankType.DexLiquidityRank
    private val periodMenu = Select(selectedPeriod, periodOptions)

    var uiState by mutableStateOf(
        UiState(
            viewState = viewState,
            rankViewItems = emptyList(),
            showPeriodMenu = showPeriodMenu,
            periodMenu = periodMenu,
        )
    )
        private set

    init {
        fetch()
    }

    fun onErrorClick() {
        viewState = ViewState.Loading
        syncState()
        fetch()
    }

    private fun syncState() {
        if (internalItems.isEmpty()) {
            uiState = UiState(
                viewState = viewState,
                rankViewItems = emptyList(),
                showPeriodMenu = showPeriodMenu,
                periodMenu = periodMenu,
            )
            return
        }

        viewModelScope.launch {
            val viewItems = withContext(Dispatchers.IO) {
                val items = internalItems.mapNotNull { internalItem ->
                    val resolvedValue: BigDecimal? = when (val anyValue = internalItem.value) {
                        is RankAnyValue.MultiValue -> {
                            when (selectedPeriod) {
                                TimeDuration.OneDay -> anyValue.rankMultiValue.value1d
                                TimeDuration.SevenDay -> anyValue.rankMultiValue.value7d
                                TimeDuration.ThirtyDay -> anyValue.rankMultiValue.value30d
                            }
                        }
                        is RankAnyValue.SingleValue -> anyValue.rankValue.value
                    }
                    resolvedValue?.let {
                        Item(internalItem.coin, it)
                    }
                }

                val sortedItems = items.sortedByDescending { it.value }

                sortedItems.mapIndexed { index, item ->
                    CoinRankModule.RankViewItem(
                        (index + 1).toString(),
                        item.coin.code,
                        item.coin.name,
                        item.coin.imageUrl,
                        formatted(item.value, baseCurrency)
                    )
                }
            }

            uiState = UiState(
                viewState = viewState,
                rankViewItems = viewItems,
                showPeriodMenu = showPeriodMenu,
                periodMenu = periodMenu,
            )
        }
    }

    private fun fetch() {
        viewModelScope.launch {
            try {
                internalItems = withContext(Dispatchers.IO) {
                    val result = getRank(rankType, baseCurrency.code)
                    val values: List<RankAnyValue> = result.mapNotNull { item ->
                        when (item) {
                            is RankMultiValue -> RankAnyValue.MultiValue(item)
                            is RankValue -> RankAnyValue.SingleValue(item)
                            else -> null
                        }
                    }

                    val coins = marketKit.allCoins()
                    val coinMap = mutableMapOf<String, Coin>()
                    coins.forEach { coinMap[it.uid] = it }
                    values.mapNotNull { anyValue ->
                        val uid = when (anyValue) {
                            is RankAnyValue.SingleValue -> anyValue.rankValue.uid
                            is RankAnyValue.MultiValue -> anyValue.rankMultiValue.uid
                        }
                        coinMap[uid]?.let { coin -> InternalItem(coin, anyValue) }
                    }
                }

                viewState = ViewState.Success
                syncState()
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            syncState()
        }
    }

    private suspend fun getRank(rankType: RankType, currencyCode: String) = withContext(Dispatchers.IO) {
        when (rankType) {
            RankType.CexVolumeRank -> marketKit.cexVolumeRanksSingle(currencyCode).await()
            RankType.DexVolumeRank -> marketKit.dexVolumeRanksSingle(currencyCode).await()
            RankType.DexLiquidityRank -> marketKit.dexLiquidityRanksSingle(currencyCode).await()
            RankType.AddressesRank -> marketKit.activeAddressRanksSingle(currencyCode).await()
            RankType.TransactionCountRank -> marketKit.transactionCountsRanksSingle(currencyCode).await()
            RankType.RevenueRank -> marketKit.revenueRanksSingle(currencyCode).await()
        }
    }

    private fun formatted(value: BigDecimal, currency: Currency): String? {
        return when (rankType) {
            RankType.CexVolumeRank,
            RankType.DexVolumeRank,
            RankType.DexLiquidityRank,
            RankType.RevenueRank -> numberFormatter.formatFiatShort(value, currency.symbol, 2)
            RankType.AddressesRank,
            RankType.TransactionCountRank -> numberFormatter.formatNumberShort(value, 0)
        }
    }

    fun toggle(period: TimeDuration) {
        selectedPeriod = period
        syncState()
    }

    data class InternalItem(
        val coin: Coin,
        val value: RankAnyValue
    )

    data class Item(
        val coin: Coin,
        val value: BigDecimal
    )
}

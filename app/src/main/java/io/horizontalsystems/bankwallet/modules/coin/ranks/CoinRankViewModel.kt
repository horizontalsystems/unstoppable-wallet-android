package io.horizontalsystems.bankwallet.modules.coin.ranks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.RankType
import io.horizontalsystems.bankwallet.modules.coin.ranks.CoinRankModule.RankAnyValue
import io.horizontalsystems.bankwallet.modules.coin.ranks.CoinRankModule.UiState
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.ui.compose.Select
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
    private val periodMenu = getPeriodMenu()

    private var sortDescending = true
    private val itemsToShow = 300

    private val header = MarketModule.Header(
        title = Translator.getString(rankType.title),
        description = Translator.getString(rankType.description),
        icon = rankType.headerIcon
    )

    var uiState by mutableStateOf(
        UiState(
            viewState = viewState,
            rankViewItems = emptyList(),
            periodSelect = periodMenu,
            header = header,
            sortDescending = sortDescending
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
                periodSelect = periodMenu,
                header = header,
                sortDescending = sortDescending
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
                                TimeDuration.ThreeMonths -> null
                            }
                        }

                        is RankAnyValue.SingleValue -> anyValue.rankValue.value
                    }
                    resolvedValue?.let {
                        Item(internalItem.coin, it)
                    }
                }

                val topItems = items.sortedByDescending { it.value }.take(itemsToShow)
                val viewItems = topItems.mapIndexed { index, item ->
                    CoinRankModule.RankViewItem(
                        item.coin.uid,
                        (index + 1).toString(),
                        item.coin.code,
                        item.coin.name,
                        item.coin.imageUrl,
                        formatted(item.value, baseCurrency)
                    )
                }
                if (sortDescending) viewItems else viewItems.reversed()
            }

            uiState = UiState(
                viewState = viewState,
                rankViewItems = viewItems,
                periodSelect = periodMenu,
                header = header,
                sortDescending = sortDescending
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
            RankType.FeeRank -> marketKit.feeRanksSingle(currencyCode).await()
            RankType.HoldersRank -> marketKit.holdersRanksSingle(currencyCode).await()
        }
    }

    private fun formatted(value: BigDecimal, currency: Currency): String {
        return when (rankType) {
            RankType.CexVolumeRank,
            RankType.DexVolumeRank,
            RankType.DexLiquidityRank,
            RankType.HoldersRank,
            RankType.RevenueRank,
            RankType.FeeRank -> numberFormatter.formatFiatShort(value, currency.symbol, 2)

            RankType.AddressesRank,
            RankType.TransactionCountRank -> numberFormatter.formatNumberShort(value, 0)
        }
    }

    private fun getPeriodMenu(): Select<TimeDuration>? = when (rankType) {
        RankType.DexLiquidityRank,
        RankType.HoldersRank -> null
        else -> Select(selectedPeriod, periodOptions)
    }

    fun toggle(period: TimeDuration) {
        selectedPeriod = period
        syncState()
    }

    fun toggleSortType() {
        sortDescending = !sortDescending
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

package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageModule.CoinViewItem
import io.horizontalsystems.bankwallet.modules.market.priceChangeValue
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class MetricsPageViewModel(
    private val metricsType: MetricsType,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) : ViewModelUiState<MetricsPageModule.UiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing: Boolean = false
    private val statPage: StatPage = metricsType.statPage
    private var viewItems: List<CoinViewItem> = listOf()
    private var sortDescending: Boolean = true
    private var marketDataJob: Job? = null

    private val toggleButtonTitle = when (metricsType) {
        MetricsType.Volume24h -> Translator.getString(R.string.Market_Volume)
        MetricsType.TotalMarketCap -> Translator.getString(R.string.Market_MarketCap)
        else -> throw Exception("MetricsType not supported")
    }

    private val title = when(metricsType) {
        MetricsType.Volume24h -> R.string.MarketGlobalMetrics_Volume
        MetricsType.TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCap
        else -> throw Exception("MetricsType not supported")
    }

    private val description = when(metricsType) {
        MetricsType.Volume24h -> R.string.MarketGlobalMetrics_VolumeDescription
        MetricsType.TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCapDescription
        else -> throw Exception("MetricsType not supported")
    }

    private val icon = when(metricsType) {
        MetricsType.Volume24h -> "total_volume"
        MetricsType.TotalMarketCap -> "total_mcap"
        else -> throw Exception("MetricsType not supported")
    }

    private val header = MarketModule.Header(
        title = Translator.getString(title),
        description = Translator.getString(description),
        icon = ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/$icon@3x.png")
    )

    override fun createState(): MetricsPageModule.UiState {
        return MetricsPageModule.UiState(
            header = header,
            viewItems = viewItems,
            viewState = viewState,
            isRefreshing = isRefreshing,
            toggleButtonTitle = toggleButtonTitle,
            sortDescending = sortDescending,
        )
    }

    init {
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                syncMarketItems()
            }
        }

        syncMarketItems()
    }

    private fun syncMarketItems() {
        marketDataJob?.cancel()
        marketDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                viewItems = getMarketItemsSingle(
                    currencyManager.baseCurrency,
                    sortDescending,
                    metricsType
                ).await()
                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun getMarketItemsSingle(
        currency: Currency,
        sortDescending: Boolean,
        metricsType: MetricsType,
        period: TimePeriod = TimePeriod.TimePeriod_1D
    ): Single<List<CoinViewItem>> {
        return marketKit.marketInfosSingle(
            250,
            currency.code,
            defi = false
        )
            .map { coinMarkets ->
                val marketItems = coinMarkets.map { marketInfo ->
                    val subtitle = when (metricsType) {
                        MetricsType.Volume24h -> CurrencyValue(
                            currency,
                            marketInfo.totalVolume ?: BigDecimal.ZERO
                        ).getFormattedShort()

                        MetricsType.TotalMarketCap -> CurrencyValue(
                            currency,
                            marketInfo.marketCap ?: BigDecimal.ZERO
                        ).getFormattedShort()

                        else -> marketInfo.fullCoin.coin.name
                    }
                    CoinViewItem(
                        fullCoin = marketInfo.fullCoin,
                        subtitle = subtitle,
                        coinRate = CurrencyValue(
                            currency,
                            marketInfo.price ?: BigDecimal.ZERO
                        ).getFormattedFull(),
                        marketDataValue = MarketDataValue.Diff(marketInfo.priceChangeValue(period)),
                        rank = marketInfo.marketCapRank?.toString(),
                        sortField = when (metricsType) {
                            MetricsType.Volume24h -> marketInfo.totalVolume
                            MetricsType.TotalMarketCap -> marketInfo.marketCap
                            else -> null
                        }
                    )
                }
                sortItems(marketItems, sortDescending)
            }
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        isRefreshing = true
        emitState()
        syncMarketItems()
        viewModelScope.launch {
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    private fun sortItems(items: List<CoinViewItem>, sortDescending: Boolean): List<CoinViewItem> {
        return if (sortDescending)
            items.sortedByDescendingNullLast { it.sortField }
        else
            items.sortedByNullLast { it.sortField }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()

        stat(page = statPage, event = StatEvent.Refresh)
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun toggleSorting() {
        sortDescending = !sortDescending
        emitState()
        if (viewItems.isNotEmpty()) {
            viewItems = sortItems(viewItems, sortDescending)
            emitState()
        } else {
            syncMarketItems()
        }
        stat(page = statPage, event = StatEvent.ToggleSortDirection)
    }

}

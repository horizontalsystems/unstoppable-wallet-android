package cash.p.terminal.modules.market.metricspage

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPage
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketDataValue
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.filters.TimePeriod
import cash.p.terminal.modules.market.priceChangeValue
import cash.p.terminal.modules.market.sortedByDescendingNullLast
import cash.p.terminal.modules.market.sortedByNullLast
import cash.p.terminal.modules.metricchart.MetricsType
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
    private var viewItems: List<MetricsPageModule.CoinViewItem> = listOf()
    private var toggleButtonTitle = when (metricsType) {
        MetricsType.Volume24h -> Translator.getString(R.string.Market_Volume)
        else -> Translator.getString(R.string.Market_MarketCap)
    }

    private var marketDataJob: Job? = null
    private var sortDescending: Boolean = true

    private val header = MarketModule.Header(
        title = Translator.getString(metricsType.title),
        description = Translator.getString(metricsType.description),
        icon = metricsType.headerIcon
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
                val coinViewItems = getMarketItemsSingle(
                    currencyManager.baseCurrency,
                    sortDescending,
                    metricsType
                ).await()
                viewState = ViewState.Success
                viewItems = coinViewItems
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
    ): Single<List<MetricsPageModule.CoinViewItem>> {
        return marketKit.marketInfosSingle(
            250,
            currency.code,
            defi = metricsType == MetricsType.DefiCap
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
                    MetricsPageModule.CoinViewItem(
                        fullCoin = marketInfo.fullCoin,
                        subtitle = subtitle,
                        coinRate = CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO).getFormattedFull(),
                        marketDataValue = MarketDataValue.Diff(marketInfo.priceChangeValue(period)),
                        rank = marketInfo.marketCapRank?.toString(),
                        sortField = when (metricsType) {
                            MetricsType.Volume24h -> marketInfo.totalVolume
                            MetricsType.TotalMarketCap -> marketInfo.marketCap
                            else -> null
                        }
                    )
                }
                if (sortDescending)
                    marketItems.sortedByDescendingNullLast { it.sortField }
                else
                    marketItems.sortedByNullLast { it.sortField }
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
        syncMarketItems()
        stat(page = statPage, event = StatEvent.ToggleSortDirection)
    }

}

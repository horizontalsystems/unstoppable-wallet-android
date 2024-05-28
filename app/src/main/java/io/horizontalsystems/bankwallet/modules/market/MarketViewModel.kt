package io.horizontalsystems.bankwallet.modules.market

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.market.MarketModule.MarketOverviewViewItem
import io.horizontalsystems.bankwallet.modules.market.MarketModule.Tab
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.marketkit.models.GlobalMarketPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class MarketViewModel(
    private val marketStorage: IMarketStorage,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    localStorage: ILocalStorage
) : ViewModelUiState<MarketModule.UiState>() {

    val tabs = Tab.entries.toTypedArray()
    private var marketOverviewJob: Job? = null
    private var marketOverviewItems: List<MarketOverviewViewItem> = listOf()
    private var selectedTab: Tab = getInitialTab(localStorage.launchPage)

    init {
        updateMarketOverview()

        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                updateMarketOverview()
            }
        }
    }

    override fun createState(): MarketModule.UiState {
        return MarketModule.UiState(
            selectedTab,
            marketOverviewItems
        )
    }

    private fun updateMarketOverview() {
        marketOverviewJob?.cancel()
        marketOverviewJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val marketOverview =
                    marketKit.marketOverviewSingle(currencyManager.baseCurrency.code).await()
                marketOverview?.globalMarketPoints?.let {
                    marketOverviewItems = getMarketMetrics(it, currencyManager.baseCurrency)
                    emitState()
                }
            } catch (e: Throwable) {
                Log.e("TAG", "updateMarketOverview: ", e)
            }
        }
    }

    fun onSelect(tab: Tab) {
        selectedTab = tab
        marketStorage.currentMarketTab = tab
        emitState()
    }

    private fun getMarketMetrics(
        globalMarketPoints: List<GlobalMarketPoint>,
        baseCurrency: Currency
    ): List<MarketOverviewViewItem> {
        var marketCap: BigDecimal? = null
        var marketCapDiff: BigDecimal? = null
        var defiMarketCap: BigDecimal? = null
        var defiMarketCapDiff: BigDecimal? = null
        var volume24h: BigDecimal? = null
        var volume24hDiff: BigDecimal? = null
        var tvl: BigDecimal? = null
        var tvlDiff: BigDecimal? = null
        var btcDominance: BigDecimal? = null
        var btcDominanceDiff: BigDecimal? = null

        if (globalMarketPoints.isNotEmpty()) {
            val startingPoint = globalMarketPoints.first()
            val endingPoint = globalMarketPoints.last()

            marketCap = endingPoint.marketCap
            marketCapDiff = diff(startingPoint.marketCap, marketCap)

            defiMarketCap = endingPoint.defiMarketCap
            defiMarketCapDiff = diff(startingPoint.defiMarketCap, defiMarketCap)

            volume24h = endingPoint.volume24h
            volume24hDiff = diff(startingPoint.volume24h, volume24h)

            btcDominance = endingPoint.btcDominance
            btcDominanceDiff = diff(startingPoint.btcDominance, btcDominance)

            tvl = endingPoint.tvl
            tvlDiff = diff(startingPoint.tvl, tvl)
        }

        val metrics: List<MarketOverviewViewItem> = listOf(
            MarketOverviewViewItem(
                "Total.Cap",
                marketCap?.let { formatFiatShortened(it, baseCurrency.symbol) } ?: "-",
                marketCapDiff?.let { getDiff(it) } ?: "----",
                marketCapDiff?.let { it > BigDecimal.ZERO } ?: false,
                MetricsType.TotalMarketCap
            ),
            MarketOverviewViewItem(
                "24h Vol.",
                volume24h?.let { formatFiatShortened(it, baseCurrency.symbol) } ?: "-",
                volume24hDiff?.let { getDiff(it) } ?: "----",
                volume24hDiff?.let { it > BigDecimal.ZERO } ?: false,
                MetricsType.Volume24h
            ),
            MarketOverviewViewItem(
                "BTC Dominance",
                btcDominance?.let {
                    App.numberFormatter.format(it, 0, 2, suffix = "%")
                } ?: "-",
                btcDominanceDiff?.let { getDiff(it) } ?: "----",
                btcDominanceDiff?.let { it > BigDecimal.ZERO } ?: false,
                MetricsType.TotalMarketCap
            ),
            MarketOverviewViewItem(
                "ETF",
                defiMarketCap?.let { formatFiatShortened(it, baseCurrency.symbol) } ?: "-",
                defiMarketCapDiff?.let { getDiff(it) } ?: "----",
                defiMarketCapDiff?.let { it > BigDecimal.ZERO } ?: false,
                MetricsType.Etf
            ),
            MarketOverviewViewItem(
                "TVL",
                tvl?.let { formatFiatShortened(it, baseCurrency.symbol) } ?: "-",
                tvlDiff?.let { getDiff(it) } ?: "----",
                tvlDiff?.let { it > BigDecimal.ZERO } ?: false,
                MetricsType.TvlInDefi
            )
        )

        return metrics
    }

    private fun getDiff(it: BigDecimal): String {
        val sign = if (it >= BigDecimal.ZERO) "+" else "-"
        return App.numberFormatter.format(it.abs(), 0, 2, sign, "%")
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        return App.numberFormatter.formatFiatShort(value, symbol, 2)
    }

    private fun diff(sourceValue: BigDecimal, targetValue: BigDecimal): BigDecimal =
        if (sourceValue.compareTo(BigDecimal.ZERO) != 0)
            ((targetValue - sourceValue) * BigDecimal(100)) / sourceValue
        else BigDecimal.ZERO


    private fun getInitialTab(launchPage: LaunchPage?) = when (launchPage) {
        LaunchPage.Watchlist -> Tab.Watchlist
        else -> marketStorage.currentMarketTab ?: Tab.Coins
    }
}
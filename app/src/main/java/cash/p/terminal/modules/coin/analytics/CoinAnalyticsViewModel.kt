package cash.p.terminal.modules.coin.analytics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartCoinValueFormatterShortened
import cash.p.terminal.modules.chart.ChartCurrencyValueFormatterShortened
import cash.p.terminal.modules.chart.ChartModule
import cash.p.terminal.modules.chart.ChartNumberFormatterShortened
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.CensorshipResistanceLevel
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.ConfiscationResistanceLevel
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.IssuanceLevel
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.PrivacyLevel
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.SecurityType
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.SecurityViewItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.ViewItem
import cash.p.terminal.modules.market.Value
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketInfoDetails
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoinAnalyticsViewModel(
    private val service: CoinAnalyticsService
) : ViewModel() {

    private val numberFormatter = ChartNumberFormatterShortened()
    private val currencyValueFormatter = ChartCurrencyValueFormatterShortened()
    private val coinValueFormatter = ChartCoinValueFormatterShortened(service.fullCoin)
    private val disposables = CompositeDisposable()

    val coin: Coin
        get() = service.fullCoin.coin

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemLiveData = MutableLiveData<ViewItem>()

    init {
        service.stateObservable
            .subscribeIO { state ->
                when (state) {
                    is DataState.Loading -> {
                        viewStateLiveData.postValue(ViewState.Loading)
                    }
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        viewItemLiveData.postValue(viewItem(state.data))
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun viewItem(item: CoinAnalyticsService.Item): ViewItem {
        return ViewItem(
            proChartsActivated = item.proCharts.activated,
            tokenLiquidityViewItem = getTokenLiquidityViewItem(item.proCharts),
            tokenDistributionViewItem = getTokenDistributionViewItem(item.proCharts, service.hasMajorHolders),
            tvlChart = chart(item.tvls, currencyValueFormatter, isMovementChart = true),
            tvlRank = item.marketInfoDetails.tvlRank?.let { "#$it" },
            tvlRatio = item.marketInfoDetails.tvlRatio?.let { App.numberFormatter.format(it, 2, 2) },
            treasuries = item.marketInfoDetails.totalTreasuries?.let {
                numberFormatter.formatValue(service.currency, it)
            },
            fundsInvested = item.marketInfoDetails.totalFundsInvested?.let {
                numberFormatter.formatValue(service.usdCurrency, it)
            },
            reportsCount = if (item.marketInfoDetails.reportsCount == 0) null else item.marketInfoDetails.reportsCount.toString(),
            securityViewItems = securityViewItems(item.marketInfoDetails),
            auditAddresses = service.auditAddresses
        )
    }

    private fun getTokenLiquidityViewItem(proCharts: CoinAnalyticsService.ProCharts): CoinAnalyticsModule.TokenLiquidityViewItem? {
        val volume = chart(proCharts.dexVolumes, currencyValueFormatter, isMovementChart = false)
        val liquidity = chart(proCharts.dexLiquidity, currencyValueFormatter, isMovementChart = true)

        if (volume == null && liquidity == null) return null

        return CoinAnalyticsModule.TokenLiquidityViewItem(volume, liquidity)
    }

    private fun getTokenDistributionViewItem(
        proCharts: CoinAnalyticsService.ProCharts,
        hasMajorHolders: Boolean
    ): CoinAnalyticsModule.TokenDistributionViewItem? {
        val txCount = chart(proCharts.txCount, numberFormatter, isMovementChart = false)
        val txVolume = chart(proCharts.txVolume, coinValueFormatter, isMovementChart = false)
        val activeAddresses = chart(proCharts.activeAddresses, numberFormatter, isMovementChart = true)

        if (txCount == null && txVolume == null && activeAddresses == null && !hasMajorHolders) return null

        return CoinAnalyticsModule.TokenDistributionViewItem(txCount, txVolume, activeAddresses, hasMajorHolders)
    }

    private fun chart(
        values: List<ChartPoint>?,
        valueFormatter: ChartModule.ChartNumberFormatter,
        isMovementChart: Boolean,
        timePeriod: HsTimePeriod? = null
    ): CoinAnalyticsModule.ChartViewItem? {
        if (values.isNullOrEmpty()) return null

        val points = values.map {
            io.horizontalsystems.chartview.models.ChartPoint(it.value.toFloat(), it.timestamp)
        }

        val chartData = ChartDataBuilder.buildFromPoints(points, isMovementChart = isMovementChart)

        val headerView = if (isMovementChart) {
            val lastItemValue = values.last().value
            val value = valueFormatter.formatValue(service.currency, lastItemValue)
            CoinAnalyticsModule.ChartHeaderView.Latest(value, Value.Percent(chartData.diff()))
        } else {
            val sum = valueFormatter.formatValue(service.currency, chartData.sum())
            CoinAnalyticsModule.ChartHeaderView.Sum(sum)
        }

        return CoinAnalyticsModule.ChartViewItem(headerView, chartData, timePeriod)
    }

    private fun chart(
        proData: CoinAnalyticsService.ProData,
        valueFormatter: ChartModule.ChartNumberFormatter,
        isMovementChart: Boolean
    ): CoinAnalyticsModule.ChartViewItem? =
        when (proData) {
            is CoinAnalyticsService.ProData.Empty,
            is CoinAnalyticsService.ProData.Forbidden -> null
            is CoinAnalyticsService.ProData.Completed -> chart(proData.chartPoints, valueFormatter, isMovementChart, proData.timePeriod)
        }

    private fun securityViewItems(marketInfoDetails: MarketInfoDetails): List<SecurityViewItem> {
        val securityViewItems = mutableListOf<SecurityViewItem>()

        marketInfoDetails.privacy?.let {
            val privacy = when (it) {
                MarketInfoDetails.SecurityLevel.Low -> PrivacyLevel.Low
                MarketInfoDetails.SecurityLevel.Medium -> PrivacyLevel.Medium
                MarketInfoDetails.SecurityLevel.High -> PrivacyLevel.High
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Privacy, privacy.title, privacy.grade))
        }

        marketInfoDetails.decentralizedIssuance?.let { decentralizedIssuance ->
            val issuance = when (decentralizedIssuance) {
                true -> IssuanceLevel.Decentralized
                false -> IssuanceLevel.Centralized
            }
            securityViewItems.add(SecurityViewItem(SecurityType.Issuance, issuance.title, issuance.grade))
        }

        marketInfoDetails.confiscationResistant?.let { confiscationResistant ->
            val resistance = when (confiscationResistant) {
                true -> ConfiscationResistanceLevel.Yes
                false -> ConfiscationResistanceLevel.No
            }
            securityViewItems.add(
                SecurityViewItem(
                    SecurityType.ConfiscationResistance,
                    resistance.title,
                    resistance.grade
                )
            )
        }

        marketInfoDetails.censorshipResistant?.let { censorshipResistant ->
            val resistance = when (censorshipResistant) {
                true -> CensorshipResistanceLevel.Yes
                false -> CensorshipResistanceLevel.No
            }
            securityViewItems.add(
                SecurityViewItem(
                    SecurityType.CensorshipResistance,
                    resistance.title,
                    resistance.grade
                )
            )
        }

        return securityViewItems
    }
}

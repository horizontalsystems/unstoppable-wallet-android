package cash.p.terminal.modules.coin.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.IAppNumberFormatter
import cash.p.terminal.core.brandColor
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.AnalyticChart
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.AnalyticInfo
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.AnalyticsViewItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.BlockViewItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.ChartViewItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.FooterItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.PreviewBlockViewItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.PreviewChartType
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.PreviewFooterItem
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule.RankType
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.metricchart.ProChartModule
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.TranslatableString.ResString
import cash.p.terminal.ui.compose.components.StackBarSlice
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class CoinAnalyticsViewModel(
    private val service: CoinAnalyticsService,
    private val numberFormatter: IAppNumberFormatter,
    private val code: String
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val currency = service.currency
    val coin: Coin
        get() = service.fullCoin.coin

    private var viewState: ViewState = ViewState.Loading
    private var analyticsViewItem: AnalyticsViewItem? = null
    private var isRefreshing = false

    var uiState by mutableStateOf(CoinAnalyticsModule.UiState(viewState))
        private set

    init {
        service.stateObservable
            .subscribeIO { state ->
                when (state) {
                    is DataState.Loading -> {
                        viewState = ViewState.Loading
                    }
                    is DataState.Success -> {
                        viewState = ViewState.Success
                        analyticsViewItem = viewItem(state.data)
                        syncState()
                    }
                    is DataState.Error -> {
                        viewState = ViewState.Error(state.error)
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
            isRefreshing = true
            syncState()
            delay(1000)
            isRefreshing = false
            syncState()
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun syncState() {
        uiState = CoinAnalyticsModule.UiState(
            viewState = viewState,
            viewItem = analyticsViewItem,
            isRefreshing = isRefreshing
        )
    }

    private fun viewItem(item: CoinAnalyticsService.AnalyticData): AnalyticsViewItem? {
        if (item.analyticsPreview != null) {
            val viewItems = getPreviewViewItems(item.analyticsPreview)
            if (viewItems.isNotEmpty()) {
                return AnalyticsViewItem.Preview(viewItems)
            }
        } else if (item.analytics != null) {
            val viewItems = getViewItems(item.analytics)
            if (viewItems.isNotEmpty()) {
                return AnalyticsViewItem.Analytics(viewItems)
            }
        }

        return AnalyticsViewItem.NoData
    }

    private fun getViewItems(analytics: Analytics): List<BlockViewItem> {
        val blocks = mutableListOf<BlockViewItem>()
        analytics.cexVolume?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_CexVolume,
                    info = AnalyticInfo.CexVolumeInfo,
                    value = getFormattedSum(data.points.map { it.volume }, currency),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = getChartViewItem(data.chartPoints(), null, false),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayRank),
                            value = getRank(data.rank30d),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.CexVolumeRank)
                        )
                    )
                )
            )
        }
        analytics.dexVolume?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_DexVolume,
                    info = AnalyticInfo.DexVolumeInfo,
                    value = getFormattedSum(data.points.map { it.volume }, currency),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = getChartViewItem(data.chartPoints(), ProChartModule.ChartType.DexVolume, false),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayRank),
                            value = getRank(data.rank30d),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.DexVolumeRank)
                        )
                    )
                )
            )
        }
        analytics.dexLiquidity?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_DexLiquidity,
                    info = AnalyticInfo.DexLiquidityInfo,
                    value = getFormattedValue(data.points.last().volume, currency),
                    valuePeriod = getValuePeriod(true),
                    analyticChart = getChartViewItem(data.chartPoints(), ProChartModule.ChartType.DexLiquidity, true),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_Rank),
                            value = getRank(data.rank),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.DexLiquidityRank)
                        )
                    )
                )
            )
        }
        analytics.addresses?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_ActiveAddresses,
                    info = AnalyticInfo.AddressesInfo,
                    value = getFormattedSum(listOf(data.count30d.toBigDecimal())),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = getChartViewItem(data.chartPoints(), ProChartModule.ChartType.AddressesCount, false),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayRank),
                            value = getRank(data.rank30d),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.AddressesRank)
                        )
                    )
                )
            )
        }
        analytics.transactions?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_TransactionCount,
                    info = AnalyticInfo.TransactionCountInfo,
                    value = getFormattedSum(data.points.map { it.count }),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = getChartViewItem(data.chartPoints(), ProChartModule.ChartType.TxVolume, false),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayVolume),
                            value = getVolume(data.volume30d)
                        ),
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayRank),
                            value = getRank(data.rank30d),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.TransactionCountRank)
                        ),
                    )
                )
            )
        }
        analytics.holders?.let { data ->
            val blockchains = service.blockchains(data.map { it.blockchainUid })
            val total = data.sumOf { it.holdersCount }
            val footerItems = mutableListOf<FooterItem>()
            val chartSlices = mutableListOf<StackBarSlice>()
            data.sortedByDescending { it.holdersCount }.forEach { item ->
                val blockchain = blockchains.firstOrNull { it.uid == item.blockchainUid }
                blockchain?.let {
                    val percent = item.holdersCount.divide(total, 4, RoundingMode.HALF_EVEN).times("100".toBigDecimal())
                    val percentFormatted = numberFormatter.format(percent, 0, 2, suffix = "%")
                    chartSlices.add(StackBarSlice(value = percent.toFloat(), color = blockchain.type.brandColor ?: Color(0xFFFFA800)))
                    footerItems.add(
                        FooterItem(
                            title = TranslatableString.PlainString(blockchain.name),
                            value = percentFormatted,
                            image = ImageSource.Remote(blockchain.type.imageUrl),
                            action = CoinAnalyticsModule.ActionType.OpenTokenHolders(coin, blockchain)
                        )
                    )
                }
            }
            if (footerItems.isEmpty()) {
                return@let
            }

            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_Holders,
                    info = AnalyticInfo.HoldersInfo,
                    value = getFormattedSum(listOf(total)),
                    valuePeriod = getValuePeriod(true),
                    analyticChart = ChartViewItem(AnalyticChart.StackedBars(chartSlices), coin.uid),
                    footerItems = footerItems
                )
            )
        }
        analytics.tvl?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_ProjectTvl,
                    info = AnalyticInfo.TvlInfo,
                    value = getFormattedValue(data.points.last().tvl, currency),
                    valuePeriod = getValuePeriod(true),
                    analyticChart = getChartViewItem(data.chartPoints(), ProChartModule.ChartType.TxVolume, true),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_Rank),
                            value = getRank(data.rank),
                            action = CoinAnalyticsModule.ActionType.OpenTvl
                        ),
                        FooterItem(ResString(R.string.CoinAnalytics_TvlRatio), numberFormatter.format(data.ratio, 2, 2)),
                    )
                )
            )
        }
        analytics.revenue?.let { data ->
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_ProjectRevenue,
                    info = AnalyticInfo.RevenueInfo,
                    value = getFormattedSum(listOf(data.value30d), currency),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = null,
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayRank),
                            value = getRank(data.rank30d),
                            action = CoinAnalyticsModule.ActionType.OpenRank(RankType.RevenueRank)
                        ),
                    )
                )
            )
        }
        if (analytics.reports != null || analytics.fundsInvested != null || analytics.treasuries != null || service.auditAddresses.isNotEmpty()) {
            val footerItems = mutableListOf<FooterItem>()
            analytics.reports?.let { reportsCount ->
                footerItems.add(
                    FooterItem(
                        title = ResString(R.string.CoinAnalytics_Reports),
                        value = reportsCount.toString(),
                        action = CoinAnalyticsModule.ActionType.OpenReports(coin.uid)
                    )
                )
            }
            analytics.fundsInvested?.let { invested ->
                footerItems.add(
                    FooterItem(
                        title = ResString(R.string.CoinAnalytics_Funding),
                        value = getFormattedValue(invested, currency),
                        action = CoinAnalyticsModule.ActionType.OpenInvestors(coin.uid)
                    )
                )
            }
            analytics.treasuries?.let { treasuries ->
                footerItems.add(
                    FooterItem(
                        title = ResString(R.string.CoinAnalytics_Treasuries),
                        value = getFormattedValue(treasuries, currency),
                        action = CoinAnalyticsModule.ActionType.OpenTreasuries(coin)
                    )
                )
            }
            if (service.auditAddresses.isNotEmpty()) {
                footerItems.add(
                    FooterItem(
                        title = ResString(R.string.Coin_Analytics_Audits),
                        action = CoinAnalyticsModule.ActionType.OpenAudits(service.auditAddresses)
                    )
                )
            }
            blocks.add(
                BlockViewItem(
                    title = null,
                    info = null,
                    analyticChart = null,
                    footerItems = footerItems,
                    sectionTitle = R.string.CoinAnalytics_OtherData,
                )
            )
        }

        return blocks
    }

    private fun getFormattedSum(values: List<BigDecimal>, currency: Currency? = null): String {
        return currency?.let { currency ->
            numberFormatter.formatFiatShort(values.sumOf { it }, currency.symbol, 2)
        } ?: numberFormatter.formatCoinShort(values.sumOf { it }, null, 0)
    }

    private fun getFormattedValue(value: BigDecimal, currency: Currency? = null): String {
        return currency?.let { currency ->
            numberFormatter.formatFiatShort(value, currency.symbol, 2)
        } ?: numberFormatter.formatCoinShort(value, null, 0)
    }

    private fun getVolume(volume30d: BigDecimal): String {
        val formatted = numberFormatter.formatNumberShort(volume30d, 1)
        return "$formatted $code"
    }

    private fun getValuePeriod(isMovement: Boolean): String {
        return Translator.getString(if (isMovement) R.string.Coin_Analytics_Current else R.string.Coin_Analytics_Last30d)
    }

    private fun getRank(rank: Int) = "#$rank"

    private fun getPreviewViewItems(analyticsPreview: AnalyticsPreview): List<PreviewBlockViewItem> {
        val blocks = mutableListOf<PreviewBlockViewItem>()
        analyticsPreview.cexVolume?.let { cexVolume ->
            if (cexVolume.points || cexVolume.rank30d) {
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_CexVolume,
                        info = AnalyticInfo.CexVolumeInfo,
                        chartType = if (cexVolume.points) PreviewChartType.Bars else null,
                        footerItems = if (cexVolume.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList()
                    )
                )
            }
        }
        analyticsPreview.dexVolume?.let { dexVolume ->
            if (dexVolume.points || dexVolume.rank30d) {
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_DexVolume,
                        info = AnalyticInfo.DexVolumeInfo,
                        chartType = if (dexVolume.points) PreviewChartType.Bars else null,
                        footerItems = if (dexVolume.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList()
                    )
                )
            }
        }
        analyticsPreview.dexLiquidity?.let { dexLiquidity ->
            if (dexLiquidity.points || dexLiquidity.rank) {
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_DexLiquidity,
                        info = AnalyticInfo.DexLiquidityInfo,
                        chartType = if (dexLiquidity.points) PreviewChartType.Line else null,
                        footerItems = if (dexLiquidity.rank) listOf(PreviewFooterItem(R.string.Coin_Analytics_Rank, true)) else emptyList()
                    )
                )
            }
        }
        analyticsPreview.addresses?.let { addresses ->
            if (addresses.points || addresses.rank30d) {
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_ActiveAddresses,
                        info = AnalyticInfo.AddressesInfo,
                        chartType = if (addresses.points) PreviewChartType.Bars else null,
                        footerItems = if (addresses.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList()
                    )
                )
            }
        }
        analyticsPreview.transactions?.let { transactionPreview ->
            if (transactionPreview.points || transactionPreview.rank30d || transactionPreview.volume30d) {
                val footerItems = mutableListOf<PreviewFooterItem>()
                if (transactionPreview.rank30d) {
                    footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true))
                }
                if (transactionPreview.volume30d) {
                    footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_30DayVolume, false))
                }
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_TransactionCount,
                        info = AnalyticInfo.TransactionCountInfo,
                        chartType = if (transactionPreview.points) PreviewChartType.Bars else null,
                        footerItems = footerItems
                    )
                )
            }
        }
        if (analyticsPreview.holders) {
            val footerItems = mutableListOf<PreviewFooterItem>(
                PreviewFooterItem(R.string.Coin_Analytics_Blockchain1, true, image = ImageSource.Local(R.drawable.ic_platform_placeholder_32)),
                PreviewFooterItem(R.string.Coin_Analytics_Blockchain2, true, image = ImageSource.Local(R.drawable.ic_platform_placeholder_32)),
                PreviewFooterItem(R.string.Coin_Analytics_Blockchain3, true, image = ImageSource.Local(R.drawable.ic_platform_placeholder_32))
            )
            blocks.add(
                PreviewBlockViewItem(
                    title = R.string.CoinAnalytics_Holders,
                    info = AnalyticInfo.HoldersInfo,
                    chartType = PreviewChartType.StackedBars,
                    footerItems = footerItems
                )
            )
        }
        analyticsPreview.revenue?.let { revenue ->
            if (revenue.value30d || revenue.rank30d) {
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_ProjectRevenue,
                        info = AnalyticInfo.RevenueInfo,
                        chartType = null,
                        footerItems = if (revenue.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList()
                    )
                )
            }
        }
        analyticsPreview.tvl?.let { tvl ->
            if (tvl.points || tvl.rank || tvl.ratio) {
                val footerItems = mutableListOf<PreviewFooterItem>()
                if (tvl.rank) {
                    footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_Rank, true))
                }
                if (tvl.ratio) {
                    footerItems.add(PreviewFooterItem(R.string.CoinAnalytics_TvlRatio, false))
                }
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_ProjectTvl,
                        info = AnalyticInfo.TvlInfo,
                        chartType = if (tvl.points) PreviewChartType.Line else null,
                        footerItems = footerItems,
                    )
                )
            }
        }
        if (analyticsPreview.reports || analyticsPreview.fundsInvested || analyticsPreview.treasuries || service.auditAddresses.isNotEmpty()) {
            val footerItems = mutableListOf<PreviewFooterItem>()
            if (analyticsPreview.reports) {
                footerItems.add(PreviewFooterItem(R.string.CoinAnalytics_Reports, true))
            }
            if (analyticsPreview.fundsInvested) {
                footerItems.add(PreviewFooterItem(R.string.CoinAnalytics_Funding, true))
            }
            if (analyticsPreview.treasuries) {
                footerItems.add(PreviewFooterItem(R.string.CoinAnalytics_Treasuries, true))
            }
            if (service.auditAddresses.isNotEmpty()) {
                footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_Audits, clickable = true, hasValue = false))
            }
            blocks.add(
                PreviewBlockViewItem(
                    title = null,
                    info = null,
                    chartType = null,
                    footerItems = footerItems,
                    sectionTitle = R.string.CoinAnalytics_OtherData,
                    showValueDots = false
                )
            )
        }

        return blocks
    }

    private fun getChartViewItem(
        values: List<ChartPoint>,
        chartType: ProChartModule.ChartType?,
        isMovementChart: Boolean,
    ): ChartViewItem? {
        if (values.isEmpty()) return null

        val points = values.map {
            io.horizontalsystems.chartview.models.ChartPoint(it.value.toFloat(), it.timestamp)
        }

        return getAnalyticChart(points, chartType, isMovementChart)
    }

    private fun getAnalyticChart(
        points: List<io.horizontalsystems.chartview.models.ChartPoint>,
        chartType: ProChartModule.ChartType?,
        isMovementChart: Boolean
    ): ChartViewItem {
        val chartData = ChartDataBuilder.buildFromPoints(points, isMovementChart = isMovementChart)

        val analyticChart = if (isMovementChart)
            AnalyticChart.Line(chartData)
        else
            AnalyticChart.Bars(chartData)

        return ChartViewItem(analyticChart, coin.uid, chartType)
    }

}

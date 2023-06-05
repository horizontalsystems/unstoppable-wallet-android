package io.horizontalsystems.bankwallet.modules.coin.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.brandColor
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticChart
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticInfo
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.AnalyticsViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.BlockViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.ChartViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.FooterItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.PreviewBlockViewItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.PreviewChartType
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.PreviewFooterItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.RankType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString.ResString
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartViewType
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

    val analyticsLink by service::analyticsLink
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
                        syncState()
                    }
                    is DataState.Success -> {
                        viewState = ViewState.Success
                        analyticsViewItem = viewItem(state.data)
                        syncState()
                    }
                    is DataState.Error -> {
                        viewState = ViewState.Error(state.error)
                        syncState()
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        viewModelScope.launch {
            service.start()
        }
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
        viewModelScope.launch {
            uiState = CoinAnalyticsModule.UiState(
                viewState = viewState,
                viewItem = analyticsViewItem,
                isRefreshing = isRefreshing
            )
        }
    }

    private fun viewItem(item: CoinAnalyticsService.AnalyticData): AnalyticsViewItem {
        if (item.analyticsPreview != null) {
            val viewItems = getPreviewViewItems(item.analyticsPreview)
            if (viewItems.isNotEmpty()) {
                val subscriptionAddress = item.analyticsPreview.subscriptions
                    ?.sortedByDescending { it.deadline }
                    ?.firstOrNull()
                    ?.address

                return AnalyticsViewItem.Preview(viewItems, subscriptionAddress)
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
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Bar, ProChartModule.ChartType.CexVolume),
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
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Bar, ProChartModule.ChartType.DexVolume),
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
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Line, ProChartModule.ChartType.DexLiquidity),
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
            val chartValue = formatNumberShort(data.points.last().count.toBigDecimal())
            blocks.add(
                BlockViewItem(
                    title = R.string.CoinAnalytics_ActiveAddresses,
                    info = AnalyticInfo.AddressesInfo,
                    value = chartValue,
                    valuePeriod = getValuePeriod(true),
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Line, ProChartModule.ChartType.AddressesCount),
                    footerItems = listOf(
                        FooterItem(
                            title = ResString(R.string.Coin_Analytics_30DayUniqueAddress),
                            value = formatNumberShort(data.count30d.toBigDecimal()),
                        ),
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
                    value = getFormattedSum(data.points.map { it.count.toBigDecimal() }),
                    valuePeriod = getValuePeriod(false),
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Bar, ProChartModule.ChartType.TxCount),
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
                            image = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.coin_placeholder),
                            action = CoinAnalyticsModule.ActionType.OpenTokenHolders(coin, blockchain)
                        )
                    )
                }
            }
            analytics.holdersRank?.let{holdersRank ->
                footerItems.add(
                    FooterItem(
                        title = ResString(R.string.CoinAnalytics_HoldersRank),
                        value = getRank(holdersRank),
                        action = CoinAnalyticsModule.ActionType.OpenRank(RankType.HoldersRank)
                    ),
                )
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
                    analyticChart = getChartViewItem(data.chartPoints(), ChartViewType.Line, ProChartModule.ChartType.Tvl),
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
                    showFooterDivider = false,
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

    private fun formatNumberShort(number: BigDecimal): String {
        return  numberFormatter.formatNumberShort(number, 1)
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
                        footerItems = if (cexVolume.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList(),
                        showFooterDivider = cexVolume.rank30d
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
                        footerItems = if (dexVolume.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList(),
                        showFooterDivider = dexVolume.rank30d
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
                        footerItems = if (dexLiquidity.rank) listOf(PreviewFooterItem(R.string.Coin_Analytics_Rank, true)) else emptyList(),
                        showFooterDivider = dexLiquidity.rank
                    )
                )
            }
        }
        analyticsPreview.addresses?.let { addresses ->
            if (addresses.points || addresses.rank30d || addresses.count30d) {
                val footerItems = mutableListOf<PreviewFooterItem>()
                if (addresses.count30d) {
                    footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_30DayUniqueAddress, false))
                }
                if (addresses.rank30d) {
                    footerItems.add(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true))
                }
                blocks.add(
                    PreviewBlockViewItem(
                        title = R.string.CoinAnalytics_ActiveAddresses,
                        info = AnalyticInfo.AddressesInfo,
                        chartType = if (addresses.points) PreviewChartType.Line else null,
                        footerItems = footerItems,
                        showFooterDivider = footerItems.isNotEmpty()
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
                        footerItems = footerItems,
                        showFooterDivider = footerItems.isNotEmpty()
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
            if (analyticsPreview.holdersRank) {
                footerItems.add(PreviewFooterItem(R.string.CoinAnalytics_HoldersRank, true))
            }
            blocks.add(
                PreviewBlockViewItem(
                    title = R.string.CoinAnalytics_Holders,
                    info = AnalyticInfo.HoldersInfo,
                    chartType = PreviewChartType.StackedBars,
                    footerItems = footerItems,
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
                        footerItems = if (revenue.rank30d) listOf(PreviewFooterItem(R.string.Coin_Analytics_30DayRank, true)) else emptyList(),
                        showFooterDivider = revenue.rank30d
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
                        showFooterDivider = footerItems.isNotEmpty()
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
                    showValueDots = false,
                    showFooterDivider = false
                )
            )
        }

        return blocks
    }

    private fun getChartViewItem(
        values: List<ChartPoint>,
        chartViewType: ChartViewType,
        chartType: ProChartModule.ChartType?,
    ): ChartViewItem? {
        if (values.isEmpty()) return null

        val points = values.map {
            io.horizontalsystems.chartview.models.ChartPoint(it.value.toFloat(), it.timestamp)
        }

        val chartData = ChartData(points, chartViewType == ChartViewType.Line, false)

        val analyticChart = when (chartViewType) {
            ChartViewType.Bar -> AnalyticChart.Bars(chartData)
            ChartViewType.Line -> AnalyticChart.Line(chartData)
        }

        return ChartViewItem(analyticChart, coin.uid, chartType)
    }

}

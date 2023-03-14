package cash.p.terminal.modules.coin.analytics

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.metricchart.ProChartModule
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.StackBarSlice
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.parcelize.Parcelize

object CoinAnalyticsModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinAnalyticsService(fullCoin, App.marketKit, App.currencyManager)

            return CoinAnalyticsViewModel(
                service,
                App.numberFormatter,
                fullCoin.coin.code
            ) as T
        }
    }

    data class BlockViewItem(
        val title: Int?,
        val info: AnalyticInfo?,
        val value: String? = null,
        val valuePeriod: String? = null,
        val analyticChart: ChartViewItem?,
        val footerItems: List<FooterItem>,
        val sectionTitle: Int? = null
    )

    data class FooterItem(
        val title: TranslatableString,
        val value: String? = null,
        val image: ImageSource? = null,
        val action: ActionType? = null
    )

    data class ChartViewItem(
        val analyticChart: AnalyticChart,
        val coinUid: String,
        val chartType: ProChartModule.ChartType? = null
    )

    sealed class AnalyticChart {
        class Line(val data: ChartData) : AnalyticChart()
        class Bars(val data: ChartData) : AnalyticChart()
        class StackedBars(val data: List<StackBarSlice>) : AnalyticChart()
    }

    data class PreviewBlockViewItem(
        val title: Int?,
        val info: AnalyticInfo?,
        val chartType: PreviewChartType?,
        val footerItems: List<PreviewFooterItem>,
        val sectionTitle: Int? = null,
        val showValueDots: Boolean = true
    )

    data class PreviewFooterItem(
        val title: Int,
        val clickable: Boolean,
        val hasValue: Boolean = true,
        val image: ImageSource? = null
    )

    enum class PreviewChartType {
        Line, Bars, StackedBars
    }

    @Parcelize
    enum class AnalyticInfo(val title: Int): Parcelable {
        CexVolumeInfo(R.string.CoinAnalytics_CexVolume),
        DexVolumeInfo(R.string.CoinAnalytics_DexVolume),
        DexLiquidityInfo(R.string.CoinAnalytics_DexLiquidity),
        AddressesInfo(R.string.CoinAnalytics_ActiveAddresses),
        TransactionCountInfo(R.string.CoinAnalytics_TransactionCount),
        HoldersInfo(R.string.CoinAnalytics_Holders),
        TvlInfo(R.string.CoinAnalytics_ProjectTvl_FullTitle),
        RevenueInfo(R.string.CoinAnalytics_ProjectRevenue),
    }

    data class UiState(
        val viewState: ViewState,
        val viewItem: AnalyticsViewItem? = null,
        val isRefreshing: Boolean = false
    )

    sealed class AnalyticsViewItem {
        class Preview(val blocks: List<PreviewBlockViewItem>) : AnalyticsViewItem()
        class Analytics(val blocks: List<BlockViewItem>) : AnalyticsViewItem()
        object NoData : AnalyticsViewItem()
    }

    sealed class ActionType {
        object OpenTvl : ActionType()
        object OpenTvlRank : ActionType()
        class OpenReports(val coinUid: String) : ActionType()
        class OpenInvestors(val coinUid: String) : ActionType()
        class OpenTreasuries(val coin: Coin) : ActionType()
        class OpenAudits(val auditAddresses: List<String>) : ActionType()
        class OpenTokenHolders(val coin: Coin, val blockchain: Blockchain) : ActionType()
    }

    fun zigzagPlaceholderAnalyticChart(isMovementChart: Boolean): AnalyticChart {
        val chartItems = mutableListOf<ChartPoint>()

        var lastTimeStamp = 0L
        for (i in 1..8) {
            val baseTimeStamp = (i * 100).toLong()
            val baseValue = i * 2

            chartItems.addAll(
                listOf(
                    ChartPoint((baseValue + 2).toFloat(), (baseTimeStamp), mapOf()),
                    ChartPoint((baseValue + 6).toFloat(), (baseTimeStamp + 25), mapOf()),
                    ChartPoint((baseValue).toFloat(), (baseTimeStamp + 50), mapOf()),
                    ChartPoint((baseValue + 9).toFloat(), (baseTimeStamp + 75), mapOf()),
                )
            )

            lastTimeStamp = baseTimeStamp + (75 + 25)
        }

        chartItems.add(ChartPoint(16f, lastTimeStamp, mapOf()))

        val chartData = ChartDataBuilder(
            points = chartItems,
            start = null,
            end = null,
            isMovementChart = isMovementChart,
            disabled = true
        ).build()

        return if (isMovementChart) AnalyticChart.Line(chartData) else AnalyticChart.Bars(chartData)
    }
}

package io.horizontalsystems.bankwallet.modules.coin.analytics

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.parcelize.Parcelize

object CoinAnalyticsModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinAnalyticsService(
                fullCoin,
                App.marketKit,
                App.currencyManager,
                App.subscriptionManager,
                App.accountManager,
                App.appConfigProvider,
            )


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
        val sectionTitle: Int? = null,
        val showFooterDivider: Boolean = true,
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
        val showValueDots: Boolean = true,
        val showFooterDivider: Boolean = true,
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

    @Parcelize
    enum class RankType(val title: Int, val description: Int, val headerIconName: String): Parcelable {
        CexVolumeRank(R.string.CoinAnalytics_CexVolumeRank, R.string.CoinAnalytics_CexVolumeRank_Description, "cex_volume" ),
        DexVolumeRank(R.string.CoinAnalytics_DexVolumeRank, R.string.CoinAnalytics_DexVolumeRank_Description, "dex_volume"),
        DexLiquidityRank(R.string.CoinAnalytics_DexLiquidityRank, R.string.CoinAnalytics_DexLiquidityRank_Description, "dex_liquidity"),
        AddressesRank(R.string.CoinAnalytics_ActiveAddressesRank, R.string.CoinAnalytics_ActiveAddressesRank_Description, "active_addresses"),
        TransactionCountRank(R.string.CoinAnalytics_TransactionCountRank, R.string.CoinAnalytics_TransactionCountRank, "trx_count"),
        RevenueRank(R.string.CoinAnalytics_ProjectRevenueRank, R.string.CoinAnalytics_ProjectRevenueRank_Description, "revenue"),
        HoldersRank(R.string.CoinAnalytics_HoldersRank, R.string.CoinAnalytics_HoldersRank_Description, "holders");

        val headerIcon: ImageSource
            get() = ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/$headerIconName@3x.png")
    }

    data class UiState(
        val viewState: ViewState,
        val viewItem: AnalyticsViewItem? = null,
        val isRefreshing: Boolean = false
    )

    sealed class AnalyticsViewItem {
        class Preview(val blocks: List<PreviewBlockViewItem>, val subscriptionAddress: String?) : AnalyticsViewItem()
        class Analytics(val blocks: List<BlockViewItem>) : AnalyticsViewItem()
        object NoData : AnalyticsViewItem()
    }

    sealed class ActionType {
        object OpenTvl : ActionType()
        class OpenRank(val type: RankType) : ActionType()
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
                    ChartPoint((baseValue + 2).toFloat(), (baseTimeStamp)),
                    ChartPoint((baseValue + 6).toFloat(), (baseTimeStamp + 25)),
                    ChartPoint((baseValue).toFloat(), (baseTimeStamp + 50)),
                    ChartPoint((baseValue + 9).toFloat(), (baseTimeStamp + 75)),
                )
            )

            lastTimeStamp = baseTimeStamp + (75 + 25)
        }

        chartItems.add(ChartPoint(16f, lastTimeStamp))

        val chartData = ChartData(chartItems, isMovementChart, true)

        return if (isMovementChart) AnalyticChart.Line(chartData) else AnalyticChart.Bars(chartData)
    }
}

package io.horizontalsystems.bankwallet.modules.coin.analytics

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.CoinIndicatorViewItemFactory
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorData
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorService
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import kotlinx.parcelize.Parcelize

object CoinAnalyticsModule {

    class Factory(private val fullCoin: FullCoin, private val apiTag: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinAnalyticsService(
                fullCoin,
                apiTag,
                App.marketKit,
                App.currencyManager,
                App.subscriptionManager,
                App.accountManager,
            )

            val indicatorsService = TechnicalIndicatorService(
                fullCoin.coin.uid,
                App.marketKit,
                App.currencyManager
            )

            return CoinAnalyticsViewModel(
                service,
                indicatorsService,
                CoinIndicatorViewItemFactory(),
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
        val sectionDescription: String? = null,
        val showFooterDivider: Boolean = true,
    )

    data class FooterItem(
        val title: BoxItem,
        val value: BoxItem? = null,
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
        class TechIndicators(val data: List<TechnicalIndicatorData>, val selectedPeriod: HsPointTimePeriod) : AnalyticChart()
    }

    enum class OverallScore(val title: Int, val icon: Int) {
        Excellent(R.string.Coin_Analytics_OverallScore_Excellent, R.drawable.ic_score_excellent_24),
        Good(R.string.Coin_Analytics_OverallScore_Good, R.drawable.ic_score_good_24),
        Fair(R.string.Coin_Analytics_OverallScore_Fair, R.drawable.ic_score_fair_24),
        Poor(R.string.Coin_Analytics_OverallScore_Poor, R.drawable.ic_score_poor_24);

        companion object {
            fun fromString(value: String?): OverallScore? = when (value) {
                "excellent" -> Excellent
                "good" -> Good
                "fair" -> Fair
                "poor" -> Poor
                else -> null
            }
        }
    }

    @Parcelize
    enum class ScoreCategory(val title: Int, val description: Int) : Parcelable {
        CexScoreCategory(R.string.CoinAnalytics_CexVolume, R.string.Coin_Analytics_OverallScore_CexVolume),
        DexVolumeScoreCategory(R.string.CoinAnalytics_DexVolume, R.string.Coin_Analytics_OverallScore_DexVolume),
        DexLiquidityScoreCategory(R.string.CoinAnalytics_DexLiquidity, R.string.Coin_Analytics_OverallScore_DexLiquidity),
        AddressesScoreCategory(R.string.CoinAnalytics_ActiveAddresses, R.string.Coin_Analytics_OverallScore_ActiveAddresses),
        TransactionCountScoreCategory(R.string.CoinAnalytics_TransactionCount, R.string.Coin_Analytics_OverallScore_TransactionCount),
        HoldersScoreCategory(R.string.CoinAnalytics_Holders, R.string.Coin_Analytics_OverallScore_Holders),
        TvlScoreCategory(R.string.CoinAnalytics_ProjectTvl, R.string.Coin_Analytics_OverallScore_ProjectTvl),
    }

    sealed class BoxItem {
        class Title(val text: TranslatableString) : BoxItem()
        class TitleWithInfo(val text: TranslatableString, val action: ActionType) : BoxItem()
        class IconTitle(val image: ImageSource, val text: TranslatableString) : BoxItem()
        class Value(val text: String) : BoxItem()
        class OverallScoreValue(val score: OverallScore) : BoxItem()
        object Dots : BoxItem()
    }

    data class PreviewBlockViewItem(
        val title: Int?,
        val info: AnalyticInfo?,
        val chartType: PreviewChartType?,
        val footerItems: List<FooterItem>,
        val sectionTitle: Int? = null,
        val showValueDots: Boolean = true,
        val showFooterDivider: Boolean = true,
    )

    enum class PreviewChartType {
        Line, Bars, StackedBars
    }

    @Parcelize
    enum class AnalyticInfo(val title: Int) : Parcelable {
        CexVolumeInfo(R.string.CoinAnalytics_CexVolume),
        DexVolumeInfo(R.string.CoinAnalytics_DexVolume),
        DexLiquidityInfo(R.string.CoinAnalytics_DexLiquidity),
        AddressesInfo(R.string.CoinAnalytics_ActiveAddresses),
        TransactionCountInfo(R.string.CoinAnalytics_TransactionCount),
        HoldersInfo(R.string.CoinAnalytics_Holders),
        TvlInfo(R.string.CoinAnalytics_ProjectTvl_FullTitle),
        TechnicalIndicatorsInfo(R.string.Coin_Analytics_TechnicalIndicators),
    }

    @Parcelize
    enum class RankType(val title: Int, val description: Int, val headerIconName: String) : Parcelable {
        CexVolumeRank(R.string.CoinAnalytics_CexVolumeRank, R.string.CoinAnalytics_CexVolumeRank_Description, "cex_volume"),
        DexVolumeRank(R.string.CoinAnalytics_DexVolumeRank, R.string.CoinAnalytics_DexVolumeRank_Description, "dex_volume"),
        DexLiquidityRank(R.string.CoinAnalytics_DexLiquidityRank, R.string.CoinAnalytics_DexLiquidityRank_Description, "dex_liquidity"),
        AddressesRank(R.string.CoinAnalytics_ActiveAddressesRank, R.string.CoinAnalytics_ActiveAddressesRank_Description, "active_addresses"),
        TransactionCountRank(R.string.CoinAnalytics_TransactionCountRank, R.string.CoinAnalytics_TransactionCountRank, "trx_count"),
        RevenueRank(R.string.CoinAnalytics_ProjectRevenueRank, R.string.CoinAnalytics_ProjectRevenueRank_Description, "revenue"),
        FeeRank(R.string.CoinAnalytics_ProjectFeeRank, R.string.CoinAnalytics_ProjectFeeRank_Description, "fee"),
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
        class Preview(val blocks: List<PreviewBlockViewItem>) : AnalyticsViewItem()
        class Analytics(val blocks: List<BlockViewItem>) : AnalyticsViewItem()
        object NoData : AnalyticsViewItem()
    }

    sealed class ActionType {
        object Preview : ActionType()
        object OpenTvl : ActionType()
        class OpenOverallScoreInfo(val scoreCategory: ScoreCategory) : ActionType()
        class OpenTechnicalIndicatorsDetails(val coinUid: String, val period: HsPointTimePeriod) : ActionType()
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

package cash.p.terminal.modules.chart

import cash.p.terminal.modules.coin.ChartInfoData
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule

data class ChartDataWrapper(
    val chartHeaderView: CoinAnalyticsModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

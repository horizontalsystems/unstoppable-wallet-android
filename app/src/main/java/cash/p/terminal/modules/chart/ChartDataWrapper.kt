package cash.p.terminal.modules.chart

import cash.p.terminal.modules.coin.ChartInfoData

data class ChartDataWrapper(
    val chartHeaderView: ChartModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

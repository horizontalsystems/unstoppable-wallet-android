package cash.p.terminal.modules.chart

import cash.p.terminal.modules.coin.ChartInfoData
import cash.p.terminal.modules.coin.details.CoinDetailsModule

data class ChartDataWrapper(
    val chartHeaderView: CoinDetailsModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

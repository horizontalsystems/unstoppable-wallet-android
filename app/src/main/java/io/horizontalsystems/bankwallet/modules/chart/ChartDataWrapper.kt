package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData

data class ChartDataWrapper(
    val chartHeaderView: ChartModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

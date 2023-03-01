package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule

data class ChartDataWrapper(
    val chartHeaderView: CoinAnalyticsModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

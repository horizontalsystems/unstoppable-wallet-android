package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule

data class ChartDataWrapper(
    val chartHeaderView: CoinDetailsModule.ChartHeaderView,
    val chartInfoData: ChartInfoData
)

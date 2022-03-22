package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value

data class ChartDataWrapper(
    val currentValue: String,
    val currentValueDiff: Value.Percent,
    val chartInfoData: ChartInfoData
)

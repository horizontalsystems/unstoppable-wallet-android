package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.chartview.Indicator
import java.math.BigDecimal

data class ChartItem(
    val value: BigDecimal,
    val dominance: BigDecimal?,
    val timestamp: Long,
    val volume: BigDecimal? = null,
    val indicators: Map<Indicator, Float?> = mapOf()
)
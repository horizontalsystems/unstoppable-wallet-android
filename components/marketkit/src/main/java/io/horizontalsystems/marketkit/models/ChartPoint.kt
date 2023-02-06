package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class ChartPoint(
    val value: BigDecimal,
    val timestamp: Long,
    val extra: Map<ChartPointType, BigDecimal>
)

enum class ChartPointType(val value: String) {
    Volume("volume")
}

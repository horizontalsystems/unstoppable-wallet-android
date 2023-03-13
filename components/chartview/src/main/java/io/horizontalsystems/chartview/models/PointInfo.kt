package io.horizontalsystems.chartview.models

import java.math.BigDecimal

class PointInfo(
    val value: Float,
    val volume: Float?,
    val dominance: BigDecimal?,
    val timestamp: Long
)

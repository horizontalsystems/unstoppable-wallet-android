package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod

data class ChartPointsWrapper(
    val chartInterval: HsTimePeriod?,
    val items: List<ChartPoint>,
    val startTimestamp: Long?,
    val endTimestamp: Long?,
    val isExpired: Boolean = false,
    val isMovementChart: Boolean = true,
) {
    constructor(chartInterval: HsTimePeriod?, items: List<ChartPoint>) : this(
        chartInterval,
        items,
        items.firstOrNull()?.timestamp,
        items.lastOrNull()?.timestamp,
    )
}
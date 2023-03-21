package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.chartview.models.ChartPoint

data class ChartPointsWrapper(
    val items: List<ChartPoint>,
    val startTimestamp: Long?,
    val endTimestamp: Long?,
    val isExpired: Boolean = false,
    val isMovementChart: Boolean = true,
) {
    constructor(items: List<ChartPoint>, isMovementChart: Boolean = true) : this(
        items,
        items.firstOrNull()?.timestamp,
        items.lastOrNull()?.timestamp,
        isMovementChart
    )
}
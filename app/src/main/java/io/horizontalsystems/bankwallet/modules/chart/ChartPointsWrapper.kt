package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint

data class ChartPointsWrapper(
    val chartType: ChartView.ChartType,
    val items: List<ChartPoint>,
    val startTimestamp: Long?,
    val endTimestamp: Long?,
    val isExpired: Boolean = false
) {
    constructor(chartType: ChartView.ChartType, items: List<ChartPoint>) : this(
        chartType,
        items,
        items.firstOrNull()?.timestamp,
        items.lastOrNull()?.timestamp,
    )
}
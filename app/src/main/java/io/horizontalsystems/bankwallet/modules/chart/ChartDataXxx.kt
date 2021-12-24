package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.chartview.ChartView

data class ChartDataXxx(
    val chartType: ChartView.ChartType,
    val items: List<ChartItem>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isExpired: Boolean = false
) {
    constructor(chartType: ChartView.ChartType, items: List<ChartItem>) : this(
        chartType,
        items,
        items.firstOrNull()?.timestamp ?: 0,
        items.lastOrNull()?.timestamp ?: 0,
    )
}
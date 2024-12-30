package io.horizontalsystems.chartview.entity

import io.horizontalsystems.chartview.ChartData

data class ChartInfoData(
    val chartData: ChartData,
    val maxValue: String?,
    val minValue: String?
)
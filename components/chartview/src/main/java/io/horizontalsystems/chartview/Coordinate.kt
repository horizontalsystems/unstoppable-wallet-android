package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.ChartPoint

data class Coordinate(
    val x: Float,
    val y: Float,
    val item: ChartPoint,
    val indicators: Map<ChartIndicatorType, Float>,
)

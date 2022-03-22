package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.PointInfo

data class Coordinate(
    val x: Float,
    val y: Float,
    val point: PointInfo,
    val item: ChartDataItemImmutable,
)

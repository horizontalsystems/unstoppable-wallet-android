package cash.p.terminal.modules.chart

import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.ChartPoint

data class ChartPointsWrapper(
    val items: List<ChartPoint>,
    val isMovementChart: Boolean = true,
    val indicators: Map<ChartIndicatorType, LinkedHashMap<Long, Float>> = mapOf()
)

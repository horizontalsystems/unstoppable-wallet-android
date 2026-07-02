package io.horizontalsystems.core.ui.extensions

import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.core.modules.metricchart.MetricsType
import java.math.BigDecimal

data class MetricData(
    val value: String?,
    val diff: BigDecimal?,
    val chartData: ChartData?,
    val type: MetricsType
)

package cash.p.terminal.ui.extensions

import cash.p.terminal.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartData
import java.math.BigDecimal

data class MetricData(
    val value: String?,
    val diff: BigDecimal?,
    val chartData: ChartData?,
    val type: MetricsType
)

package com.quantum.wallet.bankwallet.ui.extensions

import com.quantum.wallet.bankwallet.modules.metricchart.MetricsType
import com.quantum.wallet.chartview.ChartData
import java.math.BigDecimal

data class MetricData(
    val value: String?,
    val diff: BigDecimal?,
    val chartData: ChartData?,
    val type: MetricsType
)

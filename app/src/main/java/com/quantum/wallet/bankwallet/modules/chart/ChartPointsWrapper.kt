package com.quantum.wallet.bankwallet.modules.chart

import com.quantum.wallet.chartview.models.ChartIndicator
import com.quantum.wallet.chartview.models.ChartPoint

data class ChartPointsWrapper(
    val items: List<ChartPoint>,
    val isMovementChart: Boolean = true,
    val indicators: Map<String, ChartIndicator> = mapOf(),
    val customHint: String? = null
)

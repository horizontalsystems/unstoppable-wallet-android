package io.horizontalsystems.bankwallet.lib.chartview.models

import io.horizontalsystems.bankwallet.lib.chartview.ChartView

class ChartData(val points: List<Float>, val timestamp: Long, val scale: Int, val mode: ChartView.Mode)

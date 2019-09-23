package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import java.math.BigDecimal

data class ChartData(val points: List<ChartPoint>, val diff: BigDecimal)

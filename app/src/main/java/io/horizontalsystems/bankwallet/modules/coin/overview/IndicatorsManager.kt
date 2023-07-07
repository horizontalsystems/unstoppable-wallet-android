package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.MovingAverageType
import io.horizontalsystems.marketkit.models.ChartPoint
import kotlin.random.Random

class IndicatorsManager {
    fun calculateIndicators(points: List<ChartPoint>): Map<Long, Map<ChartIndicator, Float>> {
        return points.map {
            it.timestamp to mapOf<ChartIndicator, Float>(
                ChartIndicator.MovingAverage(10, MovingAverageType.SMA) to Random.nextDouble(18.0, 20.0).toFloat()
            )
        }.toMap()
    }
}

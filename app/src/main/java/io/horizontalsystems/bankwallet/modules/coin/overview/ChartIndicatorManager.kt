package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.MovingAverageType
import io.horizontalsystems.marketkit.models.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class ChartIndicatorManager {
    private val _isEnabledFlow = MutableStateFlow(false)
    val isEnabledFlow: StateFlow<Boolean>
        get() = _isEnabledFlow

    fun calculateIndicators(points: List<ChartPoint>): Map<Long, Map<ChartIndicator, Float>> {
        val chartIndicator = ChartIndicator.MovingAverage(20, MovingAverageType.SMA)

        return points.map {
            it.timestamp to mapOf<ChartIndicator, Float>(
                chartIndicator to Random.nextDouble(18.0, 20.0).toFloat()
            )
        }.toMap()
    }

    fun enable() {
        _isEnabledFlow.update { true }
    }

    fun disable() {
        _isEnabledFlow.update { false }
    }
}

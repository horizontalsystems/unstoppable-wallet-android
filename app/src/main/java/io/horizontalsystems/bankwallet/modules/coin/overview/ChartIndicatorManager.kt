package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.MovingAverageType
import io.horizontalsystems.marketkit.models.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class ChartIndicatorManager(private val localStorage: ILocalStorage) {
    private val _isEnabledFlow = MutableStateFlow(false)
    val isEnabledFlow: StateFlow<Boolean>
        get() = _isEnabledFlow

    private val _allIndicatorsFlow = MutableStateFlow(getAllIndicators())
    val allIndicatorsFlow: StateFlow<List<ChartIndicator>>
        get() = _allIndicatorsFlow

    private fun getAllIndicators(): List<ChartIndicator> {
        val enabledIds = localStorage.enabledChartIndicatorIds

        return listOf(
            ChartIndicator(
                id = "ma1",
                name = "EMA 1",
                indicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.SMA),
                enabled = enabledIds.contains("ma1"),
            ),
            ChartIndicator(
                id = "ma2",
                name = "EMA 2",
                indicatorType = ChartIndicatorType.MovingAverage(10, MovingAverageType.SMA),
                enabled = enabledIds.contains("ma2")
            ),
            ChartIndicator(
                id = "ma3",
                name = "EMA 3",
                indicatorType = ChartIndicatorType.MovingAverage(30, MovingAverageType.SMA),
                enabled = enabledIds.contains("ma3")
            ),
            ChartIndicator(
                id = "rsi",
                name = "RSI",
                indicatorType = ChartIndicatorType.Rsi,
                enabled = enabledIds.contains("rsi")
            ),
            ChartIndicator(
                id = "macd",
                name = "MACD",
                indicatorType = ChartIndicatorType.Macd,
                enabled = enabledIds.contains("macd")
            ),
        )
    }

    fun calculateIndicators(points: List<ChartPoint>): Map<Long, Map<ChartIndicatorType, Float>> {
        val chartIndicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.SMA)

        return points.map {
            it.timestamp to mapOf<ChartIndicatorType, Float>(
                chartIndicatorType to Random.nextDouble(18.0, 20.0).toFloat()
            )
        }.toMap()
    }

    fun enable() {
        _isEnabledFlow.update { true }
    }

    fun disable() {
        _isEnabledFlow.update { false }
    }

    fun enableIndicator(indicatorId: String) {
        localStorage.enabledChartIndicatorIds += indicatorId

        _allIndicatorsFlow.update {
            getAllIndicators()
        }
    }

    fun disableIndicator(indicatorId: String) {
        localStorage.enabledChartIndicatorIds -= indicatorId

        _allIndicatorsFlow.update {
            getAllIndicators()
        }
    }
}

data class ChartIndicator(
    val id: String,
    val name: String,
    val indicatorType: ChartIndicatorType,
    val enabled: Boolean
)

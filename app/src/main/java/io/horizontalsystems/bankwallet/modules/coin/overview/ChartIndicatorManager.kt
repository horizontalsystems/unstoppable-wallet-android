package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.MovingAverageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
                indicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.SMA, "#FFA800"),
                enabled = enabledIds.contains("ma1"),
            ),
            ChartIndicator(
                id = "ma2",
                name = "EMA 2",
                indicatorType = ChartIndicatorType.MovingAverage(10, MovingAverageType.SMA, "#4A98E9"),
                enabled = enabledIds.contains("ma2")
            ),
            ChartIndicator(
                id = "ma3",
                name = "EMA 3",
                indicatorType = ChartIndicatorType.MovingAverage(30, MovingAverageType.SMA, "#BF5AF2"),
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

    fun calculateIndicators(points: LinkedHashMap<Long, Float>): Map<ChartIndicatorType, LinkedHashMap<Long, Float>> {
        return getEnabledIndicators()
            .map {
                val indicatorType = it.indicatorType
                val indicatorValues = when (indicatorType) {
                    is ChartIndicatorType.MovingAverage -> {
                        calculateMovingAverage(indicatorType, points)
                    }

                    ChartIndicatorType.Macd -> {
                        LinkedHashMap()
                    }

                    ChartIndicatorType.Rsi -> {
                        LinkedHashMap()
                    }
                }
                indicatorType to indicatorValues
            }
            .toMap()
    }

    private fun calculateMovingAverage(
        movingAverage: ChartIndicatorType.MovingAverage,
        points: LinkedHashMap<Long, Float>
    ): LinkedHashMap<Long, Float> {
        val period = movingAverage.period
        val pointsList = points.toList()

        return LinkedHashMap(
            pointsList.windowed(period, 1) {
                it.last().first to it.map { it.second }.average().toFloat()
            }.toMap()
        )
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

    fun getExtraPointsCount(): Int {
        val maxPointsCount = getEnabledIndicators().maxOf { it.indicatorType.pointsCount }
        return maxPointsCount - 1
    }

    private fun getEnabledIndicators() = allIndicatorsFlow.value.filter { it.enabled }
}

data class ChartIndicator(
    val id: String,
    val name: String,
    val indicatorType: ChartIndicatorType,
    val enabled: Boolean
)

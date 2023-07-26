package io.horizontalsystems.bankwallet.modules.chart

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
                name = "SMA",
                indicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.SMA, "#FFA800"),
                enabled = enabledIds.contains("ma1"),
            ),
            ChartIndicator(
                id = "ma2",
                name = "WMA",
                indicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.WMA, "#4A98E9"),
                enabled = enabledIds.contains("ma2")
            ),
            ChartIndicator(
                id = "ma3",
                name = "EMA",
                indicatorType = ChartIndicatorType.MovingAverage(20, MovingAverageType.EMA, "#BF5AF2"),
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
        if (points.size < period) return LinkedHashMap()

        return when (movingAverage.type) {
            MovingAverageType.SMA -> calculateSMA(points, period)
            MovingAverageType.EMA -> calculateEMA(points, period)
            MovingAverageType.WMA -> calculateWMA(points, period)
        }
    }

    private fun calculateWMA(
        points: LinkedHashMap<Long, Float>,
        period: Int
    ): LinkedHashMap<Long, Float> {
        val pointsList = points.toList()
        return LinkedHashMap(
            pointsList.windowed(period, 1) { window ->
                val n = period
                val sumOfWeights = (n + 1) * n / 2
                val wma = window.mapIndexed { i, (_, value) ->
                    value * (i + 1)
                }.sum() / sumOfWeights

                window.last().first to wma
            }.toMap()
        )
    }

    private fun calculateEMA(
        points: LinkedHashMap<Long, Float>,
        period: Int
    ): LinkedHashMap<Long, Float> {
        val pointsList = points.toList()
        val subListForFirstSma = pointsList.subList(0, period)
        val firstSma = subListForFirstSma.map { it.second }.average().toFloat()

        val res = LinkedHashMap<Long, Float>()
        res[subListForFirstSma.last().first] = firstSma

        var prevEma = firstSma

        val k = 2f / (period + 1) // multiplier for weighting the EMA
        pointsList.subList(period, pointsList.size).forEach { (key, value) ->
            val ema = value * k + prevEma * (1 - k)
            res[key] = ema

            prevEma = ema
        }
        return res
    }

    private fun calculateSMA(
        points: LinkedHashMap<Long, Float>,
        period: Int
    ): LinkedHashMap<Long, Float> {
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
        val enabledIndicators = getEnabledIndicators()
        if (enabledIndicators.isEmpty()) return 0
        val maxPointsCount = enabledIndicators.maxOf { it.indicatorType.pointsCount }
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

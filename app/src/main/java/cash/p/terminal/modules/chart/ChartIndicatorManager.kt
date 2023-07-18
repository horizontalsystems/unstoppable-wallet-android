package cash.p.terminal.modules.chart
>>>>>>>> 0ac546280 (All calculation of EMA and WMA):app/src/main/java/cash/p/terminal/modules/chart/ChartIndicatorManager.kt

import io.horizontalsystems.chartview.models.ChartIndicatorType
import io.horizontalsystems.chartview.models.MovingAverageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ChartIndicatorManager(
    private val chartIndicatorSettingsDao: ChartIndicatorSettingsDao
) {
    private val _isEnabledFlow = MutableStateFlow(false)
    val isEnabledFlow: StateFlow<Boolean>
        get() = _isEnabledFlow

    val allIndicatorsFlow = chartIndicatorSettingsDao.getAll()

    init {
        if (chartIndicatorSettingsDao.getCount() == 0) {
            insertDefaultData()
        }
    }

    private fun insertDefaultData() {
        chartIndicatorSettingsDao.insertAll(ChartIndicatorSettingsDao.defaultData())
    }

    fun calculateIndicators(points: LinkedHashMap<Long, Float>): Map<ChartIndicatorType, LinkedHashMap<Long, Float>> {
        return mapOf()
//        return getEnabledIndicators()
//            .map {
//                val indicatorType = it.indicatorType
//                val indicatorValues = when (indicatorType) {
//                    is ChartIndicatorType.MovingAverage -> {
//                        calculateMovingAverage(indicatorType, points)
//                    }
//
//                    ChartIndicatorType.Macd -> {
//                        calculateMacd(points)
//                    }
//
//                    ChartIndicatorType.Rsi -> {
//                        LinkedHashMap()
//                    }
//                }
//                indicatorType to indicatorValues
//            }
//            .toMap()
    }

    private fun calculateMacd(points: LinkedHashMap<Long, Float>): LinkedHashMap<Long, Float> {
        val ema12 = calculateEMA(points, 12)
        val ema26 = calculateEMA(points, 26)

        val macdLine = LinkedHashMap(
            ema26.mapNotNull { (key, value) ->
                ema12[key]?.minus(value)?.let {
                    key to it
                }
            }.toMap()
        )
        val signalLine = calculateEMA(macdLine, 9)

        val histogram = LinkedHashMap(
            signalLine.mapNotNull { (key, value) ->
                macdLine[key]?.minus(value)?.let {
                    key to it
                }
            }.toMap()
        )

        TODO()
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
        chartIndicatorSettingsDao.enableIndicator(indicatorId)
    }

    fun disableIndicator(indicatorId: String) {
        chartIndicatorSettingsDao.disableIndicator(indicatorId)
    }

    fun getExtraPointsCount(): Int {
        val enabledIndicators = getEnabledIndicators()
        if (enabledIndicators.isEmpty()) return 0
        val maxPointsCount = enabledIndicators.maxOf { it.pointsCount }
        return maxPointsCount - 1
    }

    private fun getEnabledIndicators(): List<ChartIndicatorSetting> {
        return chartIndicatorSettingsDao.getEnabled()
    }
}

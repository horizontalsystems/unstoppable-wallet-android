package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.chartview.models.ChartIndicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ChartIndicatorManager(
    private val chartIndicatorSettingsDao: ChartIndicatorSettingsDao,
    private val localStorage: ILocalStorage
) {
    private val _isEnabledFlow = MutableStateFlow(localStorage.chartIndicatorsEnabled)
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

    fun calculateIndicators(points: LinkedHashMap<Long, Float>, startTimestamp: Long): Map<String, ChartIndicator> {
        return getEnabledIndicators()
            .mapNotNull { chartIndicatorSetting: ChartIndicatorSetting ->
                when (chartIndicatorSetting.type) {
                    ChartIndicatorSetting.IndicatorType.MA -> {
                        val typedDataMA = chartIndicatorSetting.getTypedDataMA()
                        calculateMovingAverage(points, typedDataMA, startTimestamp)
                    }
                    ChartIndicatorSetting.IndicatorType.RSI -> {
                        null
                    }
                    ChartIndicatorSetting.IndicatorType.MACD -> {
                        calculateMacd(points)
                    }
                }?.let {
                    chartIndicatorSetting.id to it
                }
            }
            .toMap()
    }

    private fun calculateMacd(points: LinkedHashMap<Long, Float>): ChartIndicator.Macd {
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

        return ChartIndicator.Macd(macdLine, signalLine, histogram)
    }

    private fun calculateMovingAverage(
        points: LinkedHashMap<Long, Float>,
        typedDataMA: ChartIndicatorDataMa,
        startTimestamp: Long
    ): ChartIndicator.MovingAverage? {
        val period = typedDataMA.period
        val maType = typedDataMA.maType
        if (points.size < period) return null

        val line = when (maType) {
            "SMA" -> calculateSMA(points, period)
            "EMA" -> calculateEMA(points, period)
            "WMA" -> calculateWMA(points, period)
            else -> return null
        }

        return ChartIndicator.MovingAverage(LinkedHashMap(line.filter { it.key >= startTimestamp }), typedDataMA.color)
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
        localStorage.chartIndicatorsEnabled = true
        _isEnabledFlow.update { true }
    }

    fun disable() {
        localStorage.chartIndicatorsEnabled = false
        _isEnabledFlow.update { false }
    }

    fun enableIndicator(indicatorId: String) {
        chartIndicatorSettingsDao.enableIndicator(indicatorId)
    }

    fun disableIndicator(indicatorId: String) {
        chartIndicatorSettingsDao.disableIndicator(indicatorId)
    }

    fun getPointsCount(): Int {
        return getEnabledIndicators().maxOfOrNull { it.pointsCount } ?: 0
    }

    private fun getEnabledIndicators(): List<ChartIndicatorSetting> {
        return chartIndicatorSettingsDao.getEnabled()
    }

    fun getChartIndicatorSetting(id: String): ChartIndicatorSetting? {
        return chartIndicatorSettingsDao.get(id)
    }

    fun update(chartIndicatorSetting: ChartIndicatorSetting) {
        chartIndicatorSettingsDao.update(chartIndicatorSetting)
    }
}

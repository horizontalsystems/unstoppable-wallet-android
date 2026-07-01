package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.chartview.models.ChartIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class ChartIndicatorManager(
    private val chartIndicatorSettingsDao: ChartIndicatorSettingsDao,
    private val localStorage: ILocalStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    val isEnabled: Boolean
        get() = localStorage.chartIndicatorsEnabled
    private val _isEnabledFlow : MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isEnabledFlow: SharedFlow<Boolean>
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
                        val typedDataRsi = chartIndicatorSetting.getTypedDataRsi()
                        calculateRsi(points, typedDataRsi, startTimestamp)
                    }
                    ChartIndicatorSetting.IndicatorType.MACD -> {
                        val typedDataMacd = chartIndicatorSetting.getTypedDataMacd()
                        calculateMacd(points, typedDataMacd, startTimestamp)
                    }
                }?.let {
                    chartIndicatorSetting.id to it
                }
            }
            .toMap()
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

        return ChartIndicator.MovingAverage(line.filterByTimestamp(startTimestamp), typedDataMA.color)
    }

    fun enable() {
        localStorage.chartIndicatorsEnabled = true
        scope.launch {
            _isEnabledFlow.emit(true)
        }
    }

    fun disable() {
        localStorage.chartIndicatorsEnabled = false
        scope.launch {
            _isEnabledFlow.emit(false)
        }
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

    companion object{
        val maPeriods = listOf(9, 25, 50)
        val rsiPeriod = 12
        val macdPeriods = listOf(12, 26, 9)

        fun calculateWMA(
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

        fun calculateEMA(
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

         fun calculateSMA(
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

        fun calculateRsi(
            points: LinkedHashMap<Long, Float>,
            typedDataRsi: ChartIndicatorDataRsi,
            startTimestamp: Long
        ): ChartIndicator.Rsi {
            val period = typedDataRsi.period

            val upMove = mutableMapOf<Long, Float>()
            val downMove = mutableMapOf<Long, Float>()

            val pointsList = points.toList()

            pointsList.windowed(2).forEach {
                val first = it.first()
                val last = it.last()
                val change = last.second - first.second
                val key = last.first
                upMove[key] = if (change > 0) change else 0f
                downMove[key] = if (change < 0) abs(change) else 0f
            }

            val rsi =  mutableMapOf<Long, Float>()

            var maUp = 0f
            var maDown = 0f
            var rStrength: Float

            var i = -1

            for (key in upMove.keys) {
                i++

                val up = upMove[key]!!
                val down = downMove[key]!!

                // SMA
                if (i < period) {
                    maUp += up
                    maDown += down
                    if (i + 1 == period) {
                        maUp /= period
                        maDown /= period
                        rStrength = maUp / maDown

                        rsi[key] = 100 - 100 / (rStrength + 1)
                    }
                    continue
                }

                // EMA
                maUp = (maUp * (period - 1) + up) / period
                maDown = (maDown * (period - 1) + down) / period
                rStrength = maUp / maDown

                rsi[key] = 100 - 100 / (rStrength + 1)
            }

            return ChartIndicator.Rsi(LinkedHashMap(rsi).filterByTimestamp(startTimestamp))
        }

         fun calculateMacd(
            points: LinkedHashMap<Long, Float>,
            typedDataMacd: ChartIndicatorDataMacd,
            startTimestamp: Long
        ): ChartIndicator.Macd {
            val emaFast = calculateEMA(points, typedDataMacd.fast)
            val emaSlow = calculateEMA(points, typedDataMacd.slow)

            val macdLine = LinkedHashMap(
                emaSlow.mapNotNull { (key, value) ->
                    emaFast[key]?.minus(value)?.let {
                        key to it
                    }
                }.toMap()
            )
            val signalLine = calculateEMA(macdLine, typedDataMacd.signal)

            val histogram = LinkedHashMap(
                signalLine.mapNotNull { (key, value) ->
                    macdLine[key]?.minus(value)?.let {
                        key to it
                    }
                }.toMap()
            )

            return ChartIndicator.Macd(
                macdLine.filterByTimestamp(startTimestamp),
                signalLine.filterByTimestamp(startTimestamp),
                histogram.filterByTimestamp(startTimestamp)
            )
        }
    }
}

private fun LinkedHashMap<Long, Float>.filterByTimestamp(startTimestamp: Long): LinkedHashMap<Long, Float> {
    return LinkedHashMap(filter { it.key >= startTimestamp })
}
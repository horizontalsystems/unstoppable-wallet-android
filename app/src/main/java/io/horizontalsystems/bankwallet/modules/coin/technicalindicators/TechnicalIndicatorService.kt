package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import io.horizontalsystems.bankwallet.core.NotEnoughData
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorDataMacd
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorDataRsi
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import kotlinx.coroutines.rx2.await
import java.io.IOException

class TechnicalIndicatorService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper,
    private val currencyKit: CurrencyManager,
) {
    companion object {
        private val maPeriods = listOf(9, 25, 50, 100, 200)
        private const val additionalPoints = 20
        private const val pointCount = 200
    }

    suspend fun fetch(period: HsPointTimePeriod): DataState<List<SectionItem>>? {
        val currency = currencyKit.baseCurrency

        return try {
            val points = marketKit.chartPointsSingle(
                coinUid = coinUid,
                currencyCode = currency.code,
                period = period,
                pointCount = pointCount + additionalPoints
            ).await()

            handle(chartPoints = points)
        } catch (e: IOException) {
            DataState.Error(e)
        }
    }

    private fun cross(value1: Float, value2: Float): Advice {
        return when {
            value1 > value2 -> Advice.BUY
            value1 < value2 -> Advice.SELL
            else -> Advice.NEUTRAL
        }
    }

    private fun handle(chartPoints: List<ChartPoint>): DataState<List<SectionItem>>? {
        try {
            val sectionItems = mutableListOf<SectionItem>()
            val chartData = calculateIndicators(chartPoints = chartPoints)

            val lastRate = chartPoints.last().value.toFloat()

            val maItems = mutableListOf<Item>()

            // Calculate ma advices
            val types = listOf("ema", "sma")
            for (type in types) {
                for (period in maPeriods) {
                    val last = getLastData("${type}_${period}", chartData)
                    val advice = last?.let { cross(lastRate, it) } ?: Advice.NODATA
                    maItems.add(Item(name = "${type.uppercase()} $period", advice = advice))
                }
            }

            // Calculate cross advices
            val ema25 = getLastData("ema_25", chartData)
            val ema50 = getLastData("ema_50", chartData)
            val crossAdvice = if (ema25 != null && ema50 != null) {
                cross(ema25, ema50)
            } else {
                Advice.NODATA
            }

            maItems.add(Item(name = "EMA Cross 25,50", advice = crossAdvice))
            sectionItems.add(SectionItem(name = "Moving Averages", items = maItems))

            val oscillatorItems = mutableListOf<Item>()

            // Calculate oscillators
            val rsiData = getLastData("rsi", chartData)
            val rsiAdvice = rsiData?.let { rsi ->
                when {
                    rsi > 70f -> Advice.SELL // overbought
                    rsi < 30f -> Advice.BUY // oversold
                    else -> Advice.NEUTRAL
                }
            } ?: Advice.NODATA
            oscillatorItems.add(Item(name = "RSI", advice = rsiAdvice))

            // Calculate MACD
            val macdData = getLastData("macd_macd", chartData)
            val macdSignalData = getLastData("macd_signal", chartData)
            val macdAdvice: Advice = if (macdData != null && macdSignalData != null) {
                cross(macdSignalData, macdData)
            } else Advice.NODATA
            oscillatorItems.add(Item(name = "MACD", advice = macdAdvice))
            sectionItems.add(SectionItem(name = "Oscillators", items = oscillatorItems))

            return DataState.Success(sectionItems)
        } catch (e: Throwable) {
            return DataState.Error(e)
        }
    }

    private fun getLastData(key: String, chartData: Map<String, java.util.LinkedHashMap<Long, Float>>): Float? {
        return try {
            chartData[key]?.let { linkedHashMap ->
                val last = linkedHashMap.toList().last()
                last.second
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateIndicators(chartPoints: List<ChartPoint>): Map<String, LinkedHashMap<Long, Float>> {
        val startTimestamp = chartPoints.firstOrNull()?.timestamp
        val endTimestamp = chartPoints.lastOrNull()?.timestamp

        if (startTimestamp == null || endTimestamp == null) {
            throw NotEnoughData()
        }

        val chartData = mutableMapOf<String, LinkedHashMap<Long, Float>>()

        val pointsForIndicators = LinkedHashMap(chartPoints.associate { it.timestamp to it.value.toFloat() })

        for (period in maPeriods) {
            try {
                val emaValues = ChartIndicatorManager.calculateEMA(period = period, points = pointsForIndicators)
                chartData["ema_$period"] = emaValues
            } catch (e: Throwable) {
                // Ignore calculation error for now
            }
            try {
                val smaValues = ChartIndicatorManager.calculateSMA(period = period, points = pointsForIndicators)
                chartData["sma_$period"] = smaValues
            } catch (e: Throwable) {
                // Ignore calculation error for now
            }
        }

        try {
            val rsiValues = ChartIndicatorManager.calculateRsi(
                points = pointsForIndicators,
                typedDataRsi = ChartIndicatorDataRsi(ChartIndicatorManager.rsiPeriod),
                startTimestamp = startTimestamp
            )
            chartData["rsi"] = rsiValues.points
        } catch (e: Throwable) {
            // Ignore calculation error for now
        }

        try {
            val chartIndicatorDataMacd = ChartIndicatorDataMacd(
                fast = ChartIndicatorManager.macdPeriods[0],
                slow = ChartIndicatorManager.macdPeriods[1],
                signal = ChartIndicatorManager.macdPeriods[2],
            )
            val macdData = ChartIndicatorManager.calculateMacd(
                points = pointsForIndicators,
                typedDataMacd = chartIndicatorDataMacd,
                startTimestamp = startTimestamp
            )
            chartData["macd_macd"] = macdData.macdLine
            chartData["macd_signal"] = macdData.signalLine
            chartData["macd_histogram"] = macdData.histogram
        } catch (e: Throwable) {
            // Ignore calculation error for now
        }

        return chartData
    }

    data class Item(
        val name: String,
        val advice: Advice
    )

    data class SectionItem(
        val name: String,
        val items: List<Item>
    )

}

package io.horizontalsystems.chartview.models

class ChartPoint(
    val value: Float,
    val timestamp: Long,
    val volume: Float? = null,
    val dominance: Float? = null,
)

sealed class ChartIndicator {
    data class MovingAverage(val line: LinkedHashMap<Long, Float>, val color: Long) : ChartIndicator()
    data class Rsi(val points: LinkedHashMap<Long, Float>) : ChartIndicator()
    data class Macd(
        val macdLine: LinkedHashMap<Long, Float>,
        val signalLine: LinkedHashMap<Long, Float>,
        val histogram: LinkedHashMap<Long, Float>
    ) : ChartIndicator()
}

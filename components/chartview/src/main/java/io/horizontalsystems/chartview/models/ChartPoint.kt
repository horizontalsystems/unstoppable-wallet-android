package io.horizontalsystems.chartview.models

class ChartPoint(
    val value: Float,
    val timestamp: Long,
    val volume: Float? = null,
    val dominance: Float? = null,
    val indicators: Map<ChartIndicator, Float> = mapOf()
)

sealed class ChartIndicator {
    data class MovingAverage(val period: Int, val type: MovingAverageType) : ChartIndicator()
    object Rsi : ChartIndicator()
    object Macd : ChartIndicator()
}

enum class MovingAverageType {
    SMA, EMA, WMA
}

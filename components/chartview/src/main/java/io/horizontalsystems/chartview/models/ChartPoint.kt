package io.horizontalsystems.chartview.models

class ChartPoint(
    val value: Float,
    val timestamp: Long,
    val volume: Float? = null,
    val dominance: Float? = null,
    val indicators: Map<ChartIndicatorType, Float> = mapOf()
)

sealed class ChartIndicatorType {
    data class MovingAverage(val period: Int, val type: MovingAverageType) : ChartIndicatorType()
    object Rsi : ChartIndicatorType()
    object Macd : ChartIndicatorType()
}

enum class MovingAverageType {
    SMA, EMA, WMA
}

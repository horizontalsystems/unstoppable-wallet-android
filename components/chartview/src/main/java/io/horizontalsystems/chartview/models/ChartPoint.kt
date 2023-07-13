package io.horizontalsystems.chartview.models

class ChartPoint(
    val value: Float,
    val timestamp: Long,
    val volume: Float? = null,
    val dominance: Float? = null,
)

sealed class ChartIndicatorType(val pointsCount: Int) {
    data class MovingAverage(
        val period: Int,
        val type: MovingAverageType,
        val color: String
    ) : ChartIndicatorType(period)
    object Rsi : ChartIndicatorType(1)
    object Macd : ChartIndicatorType(1)
}

enum class MovingAverageType {
    SMA, EMA, WMA
}

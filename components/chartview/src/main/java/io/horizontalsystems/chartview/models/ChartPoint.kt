package io.horizontalsystems.chartview.models

class ChartPoint(
    val value: Float,
    val timestamp: Long,
    val chartVolume: ChartVolume? = null,
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

data class ChartVolume(
    val value: Float,
    val type: ChartVolumeType = ChartVolumeType.Volume,
)

enum class ChartVolumeType {
    Volume,
    Tvl
}
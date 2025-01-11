package io.horizontalsystems.chartview.chart

data class SelectedItem(
    val percentagePositionX: Float,
    val timestamp: Long,
    val mainValue: Float,
    val dominance: Float?,
    val volume: Float?,
    val movingAverages: List<MA>,
    val rsi: Float?,
    val macd: Macd?
) {
    data class MA(val color: Long, val value: Float)
    data class Macd(val macdValue: Float, val signalValue: Float?, val histogramValue: Float?)
}
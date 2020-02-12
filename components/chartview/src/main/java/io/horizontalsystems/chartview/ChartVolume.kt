package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint
import kotlin.math.min

class ChartVolume(private val config: ChartConfig) {

    private var volumeBars = listOf<VolumeBar>()

    private val fillPaint = Paint()

    private var shapeHeight: Float = 0f
    private var shapeWidth: Float = 0f

    fun init(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long, shape: RectF) {
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = config.volumeRectangleColor

        shapeHeight = shape.height()
        shapeWidth = shape.width()
        volumeBars = getBars(points, startTimestamp, endTimestamp)
    }

    private fun getBars(points: List<ChartPoint>, startTimestamp: Long, endTimestamp: Long): List<VolumeBar> {
        val barList = mutableListOf<VolumeBar>()

        val deltaX = shapeWidth / (endTimestamp - startTimestamp)

        val chunks = getChunks(points)

        val volumeBarsMaxHeight = shapeHeight * config.volumeMaximumHeightRatio
        // get maximum volume to calculate Y delta
        val maxAvgVolume = chunks.map { it.avgVolume }.maxBy { it } ?: return listOf()
        val deltaY = volumeBarsMaxHeight / maxAvgVolume

        chunks.forEachIndexed { index, chunk ->
            // margin for left bar X. For first bar it's no margin
            val leftMargin = if (index != 0) config.volumeBarMargin else 0f
            // margin for right bar X. For last bar it's 0 margin
            val rightMargin = if (index != chunks.size - 1) config.volumeBarMargin else 0f

            val xLeft: Float = (chunk.start - startTimestamp) * deltaX + leftMargin
            val xRight = (chunk.end - startTimestamp) * deltaX - rightMargin

            val yTop = shapeHeight - chunk.avgVolume * deltaY
            val yBottom = shapeHeight

            barList.add(VolumeBar(xLeft, yTop, xRight, yBottom))
        }

        return barList
    }

    private fun getChunks(points: List<ChartPoint>): MutableList<VolumeChunk> {
        val lastTimestamp = points.last().timestamp

        val chunks = mutableListOf<VolumeChunk>()
        for (index in points.indices step 2) {
            val lastIndex = min(index + 2, points.size - 1) // get maximum 3 items to calculate bar

            val pointChunks: List<Pair<Long, Float>> =
                points.subList(index, lastIndex + 1).mapNotNull { chartPoint ->
                    //we only need points with volume
                    chartPoint.volume?.let {
                        chartPoint.timestamp to it
                    }
                }

            if (pointChunks.size == 3) {  // 3 points. Show standard bar
                val fromTimestamp = pointChunks[0].first
                val toTimestamp = pointChunks[2].first
                val volumeAverage = (pointChunks[0].second + pointChunks[1].second) / 2

                chunks.add(VolumeChunk(fromTimestamp, toTimestamp, volumeAverage))
            } else if (pointChunks.size == 2) {
                //last item volume is not complete, thus we only use first point volume
                val fromTimestamp = pointChunks[0].first
                val volumeAverage = pointChunks[0].second

                chunks.add(VolumeChunk(fromTimestamp, lastTimestamp, volumeAverage))
            }
        }
        return chunks
    }

    fun draw(canvas: Canvas) {
        if (volumeBars.isEmpty()) {
            return
        }

        canvas.drawVolumeBars()
    }

    private fun Canvas.drawVolumeBars() {
        volumeBars.forEach { bar ->
            val rect = RectF(bar.xLeft, config.getAnimatedY(bar.yTop, shapeHeight), bar.xRight, bar.yBottom)
            drawRect(rect, fillPaint)
        }
    }

    class VolumeChunk(val start: Long, val end: Long, val avgVolume: Float)
    class VolumeBar(
        val xLeft: Float,
        val yTop: Float,
        val xRight: Float,
        val yBottom: Float
    )
}

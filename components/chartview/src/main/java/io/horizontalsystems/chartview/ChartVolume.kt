package io.horizontalsystems.chartview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import kotlin.math.max

class ChartVolume(private val config: ChartConfig, private val animator: ChartAnimator, override var isVisible: Boolean = true) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)

    private var fromValues: LinkedHashMap<Long, Float> = linkedMapOf()
    private var fromMinKey = 0L
    private var fromMaxKey = 0L
    private var fromMinValue = 0f
    private var fromMaxValue = 0f

    private var targetValues: LinkedHashMap<Long, Float> = linkedMapOf()
    private var targetMinKey = 0L
    private var targetMaxKey = 0L
    private var targetMinValue = 0f
    private var targetMaxValue = 0f

    private var frameValues: LinkedHashMap<Long, Float> = linkedMapOf()
    private var frameMinKey = 0L
    private var frameMaxKey = 0L
    private var frameMinValue = 0f
    private var frameMaxValue = 0f

    private var combinedKeys = listOf<Long>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = config.volumeColor
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    fun setValues(values: LinkedHashMap<Long, Float>, minKey: Long, maxKey: Long) {
        targetValues = values
        targetMinKey = minKey
        targetMaxKey = maxKey
        targetMinValue = targetValues.values.minOrNull() ?: 0f
        targetMaxValue = targetValues.values.maxOrNull() ?: 0f

        fromValues = frameValues
        fromMinKey = frameMinKey
        fromMaxKey = frameMaxKey
        fromMinValue = frameMinValue
        fromMaxValue = frameMaxValue

        combinedKeys = (fromValues.keys + targetValues.keys).distinct().sorted()
    }

    private fun refreshFrameValues(animatedFraction: Float) {
        if (animatedFraction == 1f) {
            frameValues = targetValues
            frameMinKey = targetMinKey
            frameMaxKey = targetMaxKey
            frameMinValue = targetMinValue
            frameMaxValue = targetMaxValue
        } else {
            frameValues = LinkedHashMap(
                combinedKeys.map { key ->
                    val fromValue = fromValues.getOrDefault(key, targetMinValue / 10)
                    val targetValue = targetValues.getOrDefault(key, targetMinValue / 10)
                    val frameValue = changeByPercentage(fromValue, targetValue, animatedFraction)

                    key to frameValue
                }.toMap()
            )
            frameMinKey = changeByPercentage(fromMinKey, targetMinKey, animatedFraction)
            frameMaxKey = changeByPercentage(fromMaxKey, targetMaxKey, animatedFraction)
            frameMinValue = changeByPercentage(fromMinValue, targetMinValue, animatedFraction)
            frameMaxValue = changeByPercentage(fromMaxValue, targetMaxValue, animatedFraction)
        }
    }

    private fun changeByPercentage(fromValue: Float, targetValue: Float, percentage: Float): Float {
        val change = targetValue - fromValue
        return fromValue + change * percentage
    }

    private fun changeByPercentage(fromValue: Long, targetValue: Long, percentage: Float): Long {
        val change = targetValue - fromValue
        return (fromValue + change * percentage).toLong()
    }

    override fun draw(canvas: Canvas) {
        if (!isVisible) return

        refreshFrameValues(animator.animatedFraction)

        val barMinHeight = config.volumeMinHeight
        val canvasWidth = shape.width()
        val canvasHeight = shape.height()

        val timestampMin = frameMinKey
        val timestampMax = frameMaxKey
        val valueMin = frameMinValue
        val valueMax = frameMaxValue

        val xRatio = canvasWidth / (timestampMax - timestampMin)
        val yRatio = (canvasHeight - barMinHeight) / (valueMax - valueMin)

        val points = frameValues.mapNotNull { (timestamp, valueRaw) ->
            val value = valueRaw.coerceAtMost(valueMax)

            val x = (timestamp - timestampMin) * xRatio
            val y = ((value - valueMin) * yRatio + barMinHeight)

            if (y >= 0) {
                x to y
            } else {
                null
            }
        }.toMap()

        var strokeWidth = config.volumeWidth
        val pointXs = points.keys.toList()
        for (i in 0 until (pointXs.size - 1)) {
            val diff = pointXs[i + 1] - pointXs[i] - 1 // 1 is horizontal space between bars
            if (diff < strokeWidth) {
                strokeWidth = diff
            }
        }
        paint.strokeWidth = max(strokeWidth, 1f)

        points.forEach { (x, y) ->
            canvas.drawLine(x, canvasHeight, x, canvasHeight - y, paint)
        }
    }
}

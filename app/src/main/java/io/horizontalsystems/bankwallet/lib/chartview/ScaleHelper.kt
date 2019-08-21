package io.horizontalsystems.bankwallet.lib.chartview

import java.math.RoundingMode

class ScaleHelper {
    private val gridLines = 5
    private val precision = 4
    private val topBottomPadding = 0.05F

    fun scale(min: Float, max: Float): Pair<Float, Float> {
        var valueDelta = max - min
        if (valueDelta == 0f) {
            valueDelta = max
        }

        val valueMax = max + valueDelta * topBottomPadding
        val valueMin = min - valueDelta * topBottomPadding

        val valuePrecision = scalePrecision(valueMax, valueMin)
        val valueTop = setScale(valueMax, valuePrecision)
        val valueStep = setScale((valueTop - valueMin) / (gridLines - 1), valuePrecision)

        return Pair(valueTop, valueStep)
    }

    private fun scalePrecision(max: Float, min: Float): Int {
        var minValue = min
        var maxValue = max
        var count = 0

        while (count < precision) {
            if (maxValue - minValue >= gridLines) {
                return count + (if (count == 0 && maxValue < 10) 1 else 0)
            } else {
                count += 1
                minValue *= 10
                maxValue *= 10
            }
        }

        return precision
    }

    private fun setScale(value: Float, scale: Int): Float {
        val decimal = value.toBigDecimal().apply {
            setScale(scale, RoundingMode.UP)
        }

        return decimal.toFloat()
    }
}

package io.horizontalsystems.bankwallet.lib.chartview

import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import java.math.BigDecimal
import java.math.BigDecimal.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScaleHelper(private val config: ChartConfig) {
    private val gridLines = 5
    private val maxScale = 8
    private val topBottomPadding = 0.05F

    fun scale(points: List<ChartPoint>) {
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE

        for (point in points) {
            minValue = min(point.value, minValue)
            maxValue = max(point.value, maxValue)
        }

        var valueDelta = maxValue - minValue
        if (valueDelta == 0f) {
            valueDelta = maxValue
        }

        val valueMax = maxValue + valueDelta * topBottomPadding
        val valueMin = minValue - valueDelta * topBottomPadding

        val valuePrecision = setScale(valueMax.toBigDecimal(), valueMin.toBigDecimal())
        val valueTop = ceil(valueMax, valuePrecision)
        val valueStep = ceil((valueTop - valueMin) / (gridLines - 1), valuePrecision)

        config.valuePrecision = max(valuePrecision, 0)
        config.valueTop = valueTop
        config.valueStep = valueStep
    }

    private fun setScale(maxValue: BigDecimal, minValue: BigDecimal): Int {
        val intDigits = String.format("%.0f", maxValue).length

        var min = minValue.divide(TEN.pow(intDigits))
        var max = maxValue.divide(TEN.pow(intDigits))
        var count = -intDigits

        while (count < maxScale) {
            if ((max - min).toInt() >= gridLines) {
                return count + (if (count == 0 && maxValue < TEN) 1 else 0)
            } else {
                count += 1
                min = min.multiply(TEN)
                max = max.multiply(TEN)
            }
        }

        return maxScale
    }

    private fun ceil(value: Float, scale: Int): Float {
        val valueDec = value.toBigDecimal()
        val scalePow = if (scale < 0) {
            ONE.divide(TEN.pow(abs(scale)))
        } else {
            ONE.multiply(TEN.pow(abs(scale)))
        }

        val multipliedValue = valueDec.multiply(scalePow)
        var multipliedIntegerValue = valueOf(multipliedValue.toLong())

        if (multipliedValue - multipliedIntegerValue > ZERO) {
            multipliedIntegerValue += ONE
        }

        return (multipliedIntegerValue.divide(scalePow)).toFloat()
    }
}

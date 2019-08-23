package io.horizontalsystems.bankwallet.lib.chartview

import java.math.BigDecimal
import java.math.BigDecimal.*
import kotlin.math.abs

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

        val valuePrecision = scalePrecision(valueMax.toBigDecimal(), valueMin.toBigDecimal())
        val valueTop = ceil(valueMax, valuePrecision)
        val valueStep = ceil((valueTop - valueMin) / (gridLines - 1), valuePrecision)

        return Pair(valueTop, valueStep)
    }

    private fun scalePrecision(maxValue: BigDecimal, minValue: BigDecimal): Int {
        val intDigits = String.format("%.0f", maxValue).length

        var min = minValue.divide(TEN.pow(intDigits))
        var max = maxValue.divide(TEN.pow(intDigits))
        var count = -intDigits

        while (count < precision) {
            if ((max - min).toInt() >= gridLines) {
                return count + (if (count == 0 && maxValue < TEN) 1 else 0)
            } else {
                count += 1
                min = min.multiply(TEN)
                max = max.multiply(TEN)
            }
        }

        return precision
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

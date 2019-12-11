package io.horizontalsystems.bankwallet.lib.chartview

import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import java.math.BigDecimal
import java.math.BigDecimal.TEN
import kotlin.math.max
import kotlin.math.min

class ScaleHelper(private val config: ChartConfig) {
    private val maxScale = 8
    private val minDigitDiff = 5
    private val verticalPadding = 0.18F

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

        config.valueTop = (maxValue + valueDelta * verticalPadding)
        config.valueLow = (minValue - valueDelta * verticalPadding)
        config.valueScale = max(getScale(maxValue.toBigDecimal(), minValue.toBigDecimal()), 0)
    }

    private fun getScale(maxValue: BigDecimal, minValue: BigDecimal): Int {
        val intDigits = String.format("%.0f", maxValue).length

        var min = minValue.divide(TEN.pow(intDigits))
        var max = maxValue.divide(TEN.pow(intDigits))
        var count = -intDigits

        while (count < maxScale) {
            if ((max - min).toInt() >= minDigitDiff) {
                return count + (if (count == 0 && maxValue < TEN) 1 else 0)
            } else {
                count += 1
                min = min.multiply(TEN)
                max = max.multiply(TEN)
            }
        }

        return maxScale
    }
}

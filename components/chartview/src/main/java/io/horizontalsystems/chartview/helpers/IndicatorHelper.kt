package io.horizontalsystems.chartview.helpers

import java.math.MathContext
import kotlin.math.abs

object IndicatorHelper {
    fun ema(values: List<Float>, period: Int): List<Float?> {
        if (values.size < period) return listOf()

        val k = 2f / (period + 1) // multiplier for weighting the EMA

        var ma = 0f
        val maList = arrayOfNulls<Float?>(period - 1).toMutableList()

        for (i in values.indices) {
            val price = values[i]

            if (i < period) {
                ma += price
                continue
            }

            if (i == period) {
                ma /= period
                maList.add(ma)
            }

            ma = price * k + ma * (1 - k)
            maList.add(ma)
        }

        return maList
    }

    fun rsi(values: List<Float>, period: Int): List<Float?> {
        val upMove = mutableListOf<Float>()
        val downMove = mutableListOf<Float>()

        for (i in 1 until values.size) {
            val change = values[i] - values[i - 1]
            upMove.add(if (change > 0) change else 0f)
            downMove.add(if (change < 0) abs(change) else 0f)
        }

        val emaUp = mutableListOf<Float>()
        val emaDown = mutableListOf<Float>()
        val rsi = mutableListOf<Float>()

        var maUp = 0f
        var maDown = 0f
        var rStrength: Float

        for (i in 0 until upMove.size) {
            val up = upMove[i]
            val down = downMove[i]

            // SMA
            if (i < period) {
                maUp += up
                maDown += down
                continue
            }

            if (i == period) {
                maUp /= period
                maDown /= period
                rStrength = maUp / maDown

                emaUp.add(maUp)
                emaDown.add(maDown)
                rsi.add(100 - 100 / (rStrength + 1))
            }

            // EMA
            maUp = (maUp * (period - 1) + up) / period
            maDown = (maDown * (period - 1) + down) / period
            rStrength = maUp / maDown

            emaUp.add(maUp)
            emaDown.add(maDown)
            rsi.add(100 - 100 / (rStrength + 1))
        }

        return prependWithNulls(rsi, values.size)
    }

    fun macd(values: List<Float>, fastPeriods: Int = 12, slowPeriods: Int = 26, signalPeriods: Int = 9): Triple<List<Float?>, List<Float?>, List<Float?>> {
        val emaFast = ema(values, fastPeriods)
        val emaSlow = ema(values, slowPeriods)

        val macd = emaFast.zip(emaSlow) { f, s ->
            when {
                f == null -> null
                s == null -> null
                else -> f - s
            }
        }

        val valuesSize = values.size
        val signal = prependWithNulls(ema(macd.filterNotNull(), signalPeriods), valuesSize)

        val histogram = macd.zip(signal) { m, s ->
            when {
                m == null -> null
                s == null -> null
                else -> (m - s).toBigDecimal(MathContext(5)).toFloat()
            }
        }

        return Triple(macd, signal, histogram)
    }

    private fun prependWithNulls(
        list: List<Float?>,
        size: Int,
    ) = List<Float?>(size - list.size) { null } + list
}

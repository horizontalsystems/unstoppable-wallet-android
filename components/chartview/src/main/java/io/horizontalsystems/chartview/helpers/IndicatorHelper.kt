package io.horizontalsystems.chartview.helpers

import java.math.MathContext
import kotlin.math.abs

object IndicatorHelper {
    fun ema(values: List<Float>, period: Int): List<Float> {
        val k = 2f / (period + 1) // multiplier for weighting the EMA

        var ma = 0f
        val maList = mutableListOf<Float>()

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

    fun rsi(values: List<Float>, period: Int): List<Float> {
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

        return rsi
    }

    fun macd(values: List<Float>, fastPeriods: Int = 12, slowPeriods: Int = 26, signalPeriods: Int = 9): Triple<List<Float>, List<Float>, List<Float>> {
        val emaFast = ema(values, fastPeriods)
        val emaSlow = ema(values, slowPeriods)

        val macd = mutableListOf<Float>()

        val startEmaSlow = emaFast.size - emaSlow.size
        for (i in emaSlow.indices) {
            val item = emaFast[startEmaSlow + i]
            macd.add(item - emaSlow[i])
        }

        val signal = ema(macd, signalPeriods)
        val histogram = mutableListOf<Float>()

        val startHistogram = macd.size - signal.size
        for (i in signal.indices) {
            val item = macd[startHistogram + i]
            val new = (item - signal[i]).toBigDecimal(MathContext(5))
            histogram.add(new.toFloat())
        }

        return Triple(macd, signal, histogram)
    }
}

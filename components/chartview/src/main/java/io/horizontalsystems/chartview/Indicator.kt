package io.horizontalsystems.chartview

open class Indicator(val name: String) {
    object Candle : Indicator("candle")
    object Volume : Indicator("volume")

    object EmaFast : Indicator("emaFast") {
        const val period = 25
    }

    object EmaSlow : Indicator("emaSlow") {
        const val period = 50
    }

    object Rsi : Indicator("rsi") {
        const val period = 14
        const val max = 70
        const val min = 30
    }

    object Macd : Indicator("macd") {
        const val fastPeriod = 12
        const val slowPeriod = 26
        const val signalPeriod = 9
    }

    object MacdSignal : Indicator("macdSignal")
    object MacdHistogram : Indicator("MacdHistogram")

    override fun equals(other: Any?): Boolean {
        if (other is Indicator) {
            return name == other.name
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

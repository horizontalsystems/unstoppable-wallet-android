package io.horizontalsystems.chartview

sealed class Indicator(val name: String) {
    object Candle : Indicator("candle")
    object Volume : Indicator("volume")
    object Dominance : Indicator("dominance")

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

package io.horizontalsystems.marketkit.models

enum class HsTimePeriod(val value: String) {
    Day1("1d"),
    Week1("1w"),
    Week2("2w"),
    Month1("1m"),
    Month3("3m"),
    Month6("6m"),
    Year1("1y"),
    Year2("2y");

    val range: Long
        get() = when (this) {
            Day1 -> day
            Week1 -> 7 * day
            Week2 -> 14 * day
            Month1 -> 30 * day
            Month3 -> 90 * day
            Month6 -> 180 * day
            Year1 -> 365 * day
            Year2 -> 730 * day
        }

    private val day = (24 * 60 * 60).toLong()
}

sealed class HsPeriodType {

    data class ByPeriod(val timePeriod: HsTimePeriod) : HsPeriodType()
    data class ByStartTime(val startTime: Long) : HsPeriodType()

    val range: Long?
        get() = when (this) {
            is ByPeriod -> timePeriod.range
            is ByStartTime -> null
        }

    fun serialize() = when (this) {
        is ByPeriod -> "period:${timePeriod.value}"
        is ByStartTime -> "startTime:$startTime"
    }

    companion object {
        fun deserialize(v: String): HsPeriodType? {
            val (type, value) = v.split(":")

            return when (type) {
                "period" -> {
                    HsTimePeriod
                        .values()
                        .firstOrNull { it.value == value }
                        ?.let { ByPeriod(it) }
                }
                "startTime" -> ByStartTime(value.toLong())
                else -> null
            }
        }
    }


}
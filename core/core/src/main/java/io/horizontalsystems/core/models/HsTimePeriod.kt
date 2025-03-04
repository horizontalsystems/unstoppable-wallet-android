package io.horizontalsystems.core.models

enum class HsTimePeriod(val value: String) {
    Hour1("1h"),
    Day1("1d"),
    Week1("1w"),
    Month1("1m"),
    Year1("1y");

    val range: Long
        get() = when (this) {
            Hour1 -> 60 * 60
            Day1 -> day
            Week1 -> 7 * day
            Month1 -> 30 * day
            Year1 -> 365 * day
        }

    private val day = (24 * 60 * 60).toLong()
}

sealed class HsPeriodType(open val timePeriod: HsTimePeriod?) {

    data class ByPeriod(override val timePeriod: HsTimePeriod) : HsPeriodType(timePeriod)
    data class ByCustomPoints(override val timePeriod: HsTimePeriod, val pointsCount: Int) : HsPeriodType(timePeriod)
    data class ByStartTime(val startTime: Long) : HsPeriodType(null)

    val range: Long?
        get() = when (this) {
            is ByPeriod -> timePeriod.range
            is ByStartTime -> null
            is ByCustomPoints -> timePeriod.range
        }

    fun serialize() = when (this) {
        is ByPeriod -> "period:${timePeriod.value}"
        is ByStartTime -> "startTime:$startTime"
        is ByCustomPoints -> "customPoints:${timePeriod.value}:$pointsCount"
    }

    companion object {
        fun deserialize(v: String): HsPeriodType? {
            val (type, value) = v.split(":", limit = 2)

            return when (type) {
                "period" -> {
                    HsTimePeriod
                        .values()
                        .firstOrNull { it.value == value }
                        ?.let { ByPeriod(it) }
                }
                "startTime" -> ByStartTime(value.toLong())
                "customPoints" -> {
                    val (timePeriod, pointsCount) = value.split(":")

                    HsTimePeriod
                        .values()
                        .firstOrNull { it.value == timePeriod }
                        ?.let {
                            ByCustomPoints(it, pointsCount.toInt())
                        }
                }
                else -> null
            }
        }
    }


}
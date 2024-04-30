package io.horizontalsystems.marketkit.chart

import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.HsTimePeriod
import java.util.*

object HsChartRequestHelper {

    fun pointInterval(periodType: HsPeriodType) = when (periodType) {
        is HsPeriodType.ByPeriod -> {
            byPeriod(periodType.timePeriod)
        }
        is HsPeriodType.ByCustomPoints -> {
            byPeriod(periodType.timePeriod)
        }
        is HsPeriodType.ByStartTime -> {
            val currentTime = Date().time / 1000
            val seconds = currentTime - periodType.startTime

            when {
                seconds <= HsTimePeriod.Day1.range -> HsPointTimePeriod.Minute30
                seconds <= HsTimePeriod.Week1.range -> HsPointTimePeriod.Hour4
                seconds <= HsTimePeriod.Week2.range -> HsPointTimePeriod.Hour8
                seconds <= HsTimePeriod.Year1.range -> HsPointTimePeriod.Day1
                seconds <= HsTimePeriod.Year5.range -> HsPointTimePeriod.Week1
                else -> HsPointTimePeriod.Month1
            }
        }
    }

    private fun byPeriod(timePeriod: HsTimePeriod) = when (timePeriod) {
        HsTimePeriod.Day1 -> HsPointTimePeriod.Minute30
        HsTimePeriod.Week1 -> HsPointTimePeriod.Hour4
        HsTimePeriod.Week2 -> HsPointTimePeriod.Hour8
        HsTimePeriod.Month1,
        HsTimePeriod.Month3,
        HsTimePeriod.Month6 -> HsPointTimePeriod.Day1
        HsTimePeriod.Year1,
        HsTimePeriod.Year2 -> HsPointTimePeriod.Week1
        HsTimePeriod.Year5 -> HsPointTimePeriod.Month1
    }

    fun fromTimestamp(timestamp: Long, periodType: HsPeriodType) = when (periodType) {
        is HsPeriodType.ByPeriod -> {
            timestamp - periodType.timePeriod.range
        }
        is HsPeriodType.ByCustomPoints -> {
            timestamp - periodType.timePeriod.range
        }
        is HsPeriodType.ByStartTime -> {
            periodType.startTime
        }

    }
}
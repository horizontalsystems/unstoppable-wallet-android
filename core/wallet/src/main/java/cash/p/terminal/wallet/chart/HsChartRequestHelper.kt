package cash.p.terminal.wallet.chart

import io.horizontalsystems.core.models.HsPeriodType
import cash.p.terminal.wallet.models.HsPointTimePeriod
import io.horizontalsystems.core.models.HsTimePeriod
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
                seconds <= HsTimePeriod.Hour1.range -> HsPointTimePeriod.Minute30
                seconds <= HsTimePeriod.Day1.range -> HsPointTimePeriod.Minute30
                seconds <= HsTimePeriod.Week1.range -> HsPointTimePeriod.Hour4
                seconds <= HsTimePeriod.Year1.range -> HsPointTimePeriod.Day1
                else -> HsPointTimePeriod.Month1
            }
        }
    }

    private fun byPeriod(timePeriod: HsTimePeriod) = when (timePeriod) {
        HsTimePeriod.Hour1 -> HsPointTimePeriod.Minute30
        HsTimePeriod.Day1 -> HsPointTimePeriod.Minute30
        HsTimePeriod.Week1 -> HsPointTimePeriod.Hour4
        HsTimePeriod.Month1  -> HsPointTimePeriod.Day1
        HsTimePeriod.Year1 -> HsPointTimePeriod.Week1
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
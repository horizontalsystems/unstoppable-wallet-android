package io.horizontalsystems.chartview.helpers

import android.text.format.DateFormat.getBestDateTimePattern
import io.horizontalsystems.chartview.models.GridColumn
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import kotlin.math.ceil

object GridHelper {

    fun map(startTime: Long, endTime: Long, width: Float): List<GridColumn> {
        val start = startTime * 1000
        val end = endTime * 1000

        val days = Duration.ofSeconds(endTime - startTime).toDays()

        val calendar = Calendar.getInstance().apply { time = Date() }
        var columnLabel = columnLabel(calendar, days)

        moveLastLine(calendar, days)

        val delta = (end - start) / width
        val columns = mutableListOf<GridColumn>()

        while (true) {
            val xAxis = (calendar.time.time - start) / delta
            if (xAxis <= 0) break

            columns.add(GridColumn(xAxis, columnLabel))
            moveColumn(calendar, days)
            columnLabel = columnLabel(calendar, days)
        }

        return columns
    }

    private fun moveLastLine(calendar: Calendar, days: Long) {
        //  We need to move last vertical grid line to nearest hour/day depending on chart type
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (days > 1) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
        }
        if (days > 180) {
            calendar.set(Calendar.DATE, 1)
        }
    }

    private fun moveColumn(calendar: Calendar, days: Long) = when {
        days <= 1  -> calendar.add(Calendar.HOUR_OF_DAY, -6)
        days <= 7 -> calendar.add(Calendar.DAY_OF_WEEK, -2)
        days <= 14 -> calendar.add(Calendar.DAY_OF_WEEK, -3)
        days <= 30 -> calendar.add(Calendar.DAY_OF_MONTH, -6)
        days <= 100 -> calendar.add(Calendar.DAY_OF_MONTH, -14)
        days <= 180 -> calendar.add(Calendar.MONTH, -1)
        days <= 365 -> calendar.add(Calendar.MONTH, -2)
        days <= 730 -> calendar.add(Calendar.MONTH, -4)
        else -> {
            val months = days / 30
            val amount = ceil(months / 6.0).toInt()
            calendar.add(Calendar.MONTH, amount * -1)
        }
    }

    private fun columnLabel(calendar: Calendar, days: Long) = when {
        days <= 1 -> calendar.get(Calendar.HOUR_OF_DAY).toString()
        days <= 7 -> formatDate(calendar.time, "EEE")
        days <= 100 -> calendar.get(Calendar.DAY_OF_MONTH).toString()
        days <= 730 -> formatDate(calendar.time, "MMM")
        else -> formatDate(calendar.time, "MMM")
    }

    private fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(
            getBestDateTimePattern(Locale.getDefault(), pattern),
            Locale.getDefault()
        ).format(date)
    }
}

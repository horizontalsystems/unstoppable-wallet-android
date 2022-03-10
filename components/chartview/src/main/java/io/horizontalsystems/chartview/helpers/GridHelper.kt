package io.horizontalsystems.chartview.helpers

import android.text.format.DateFormat.getBestDateTimePattern
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.models.GridColumn
import java.text.SimpleDateFormat
import java.util.*

object GridHelper {

    fun map(chartType: ChartType, startTime: Long, endTime: Long, width: Float): List<GridColumn> {
        val start = startTime * 1000
        val end = endTime * 1000

        val calendar = Calendar.getInstance().apply { time = Date() }
        var columnLabel = columnLabel(calendar, chartType)

        //  We need to move last vertical grid line to nearest hour/day depending on chart type
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        when (chartType) {
            ChartType.Day1 -> {}
            ChartType.Week1,
            ChartType.Week2,
            ChartType.Month1 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            ChartType.Month3,
            ChartType.Month6,
            ChartType.Year1 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.DATE, 1)
            }
        }

        val delta = (end - start) / width
        val columns = mutableListOf<GridColumn>()

        while (true) {
            val xAxis = (calendar.time.time - start) / delta
            if (xAxis <= 0) break

            columns.add(GridColumn(xAxis, columnLabel))
            moveColumn(chartType, calendar)
            columnLabel = columnLabel(calendar, chartType)
        }

        return columns
    }

    private fun moveColumn(type: ChartType, calendar: Calendar) {
        when (type) {
            ChartType.Day1 -> calendar.add(Calendar.HOUR_OF_DAY, -6)       // 6 hour
            ChartType.Week1 -> calendar.add(Calendar.DAY_OF_WEEK, -2)      // 2 days
            ChartType.Week2 -> calendar.add(Calendar.DAY_OF_WEEK, -3)      // 3 days
            ChartType.Month1 -> calendar.add(Calendar.DAY_OF_MONTH, -6)    // 6 days
            ChartType.Month3 -> calendar.add(Calendar.DAY_OF_MONTH, -14)  // 6 days
            ChartType.Month6 -> calendar.add(Calendar.MONTH, -1)          // 1 month
            ChartType.Year1 -> calendar.add(Calendar.MONTH, -2)         // 2 month
        }
    }

    private fun columnLabel(calendar: Calendar, type: ChartType): String {
        return when (type) {
            ChartType.Day1 -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            ChartType.Week1 -> formatDate(calendar.time, "EEE")
            ChartType.Week2,
            ChartType.Month1,
            ChartType.Month3 -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            ChartType.Month6,
            ChartType.Year1 -> formatDate(calendar.time, "MMM")
        }
    }

    private fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(
            getBestDateTimePattern(Locale.getDefault(), pattern),
            Locale.getDefault()
        ).format(date)
    }
}

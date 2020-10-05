package io.horizontalsystems.chartview.helpers

import android.text.format.DateFormat.getBestDateTimePattern
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.Coordinate
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
            ChartType.TODAY,
            ChartType.DAILY -> {
            }
            ChartType.WEEKLY,
            ChartType.WEEKLY2,
            ChartType.MONTHLY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            ChartType.MONTHLY3,
            ChartType.MONTHLY6,
            ChartType.MONTHLY12,
            ChartType.MONTHLY24 -> {
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
            ChartType.TODAY -> calendar.add(Calendar.HOUR_OF_DAY, -6)       // 6 hour
            ChartType.DAILY -> calendar.add(Calendar.HOUR_OF_DAY, -6)       // 6 hour
            ChartType.WEEKLY -> calendar.add(Calendar.DAY_OF_WEEK, -2)      // 2 days
            ChartType.WEEKLY2 -> calendar.add(Calendar.DAY_OF_WEEK, -3)      // 3 days
            ChartType.MONTHLY -> calendar.add(Calendar.DAY_OF_MONTH, -6)    // 6 days
            ChartType.MONTHLY3 -> calendar.add(Calendar.DAY_OF_MONTH, -14)  // 6 days
            ChartType.MONTHLY6 -> calendar.add(Calendar.MONTH, -1)          // 1 month
            ChartType.MONTHLY12 -> calendar.add(Calendar.MONTH, -2)         // 2 month
            ChartType.MONTHLY24 -> calendar.add(Calendar.MONTH, -4)         // 4 month
        }
    }

    private fun columnLabel(calendar: Calendar, type: ChartType): String {
        return when (type) {
            ChartType.TODAY,
            ChartType.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            ChartType.WEEKLY -> formatDate(calendar.time, "EEE")
            ChartType.WEEKLY2,
            ChartType.MONTHLY,
            ChartType.MONTHLY3 -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            ChartType.MONTHLY6,
            ChartType.MONTHLY12,
            ChartType.MONTHLY24 -> formatDate(calendar.time, "MMM")
        }
    }

    private fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault()).format(date)
    }
}

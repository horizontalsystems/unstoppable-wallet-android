package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine
import java.sql.Date
import java.util.*

class GridHelper(private val shape: RectF, private val config: ChartConfig) {

    private val daysInMonth = 30
    private val daysInYear = 364
    private val minsInDay = 24 * 60

    fun setGridLines(): List<GridLine> {
        var y: Float
        var value = config.valueTop
        val gridLines = mutableListOf<GridLine>()

        repeat(4) {
            val gridSpacing = shape.bottom / 4
            y = gridSpacing * it + shape.top

            gridLines.add(GridLine(y, String.format("%.${config.valuePrecision}f", value)))
            value -= config.valueStep
        }

        return gridLines
    }

    fun setGridColumns(data: ChartData): List<GridColumn> {
        val date = Date(data.timestamp * 1000)
        val calendar = Calendar.getInstance().apply { time = date }

        val interval = intervalByMinutes(data.mode)
        val intervalMillis = interval * 60 * 1000L

        val minutes = minutesInMode(data.mode)
        val minutesPerPx = shape.right / minutes

        val mins = calendar.get(Calendar.MINUTE)
        val secs = calendar.get(Calendar.SECOND)

        var point = mins + (secs / 60f)

        when (data.mode) {
            ChartType.DAILY -> {
            }
            ChartType.WEEKLY,
            ChartType.MONTHLY -> {
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                point += hours * 60
            }
            ChartType.MONTHLY6,
            ChartType.MONTHLY18 -> {
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                val days = calendar.get(Calendar.DAY_OF_MONTH)
                point += days * 24 * 60
                point += hours * 60
            }
        }

        val columns = mutableListOf<GridColumn>()
        while (point <= minutes) {
            val offset = point * minutesPerPx
            columns.add(GridColumn(shape.right - offset, pointName(calendar, data.mode)))
            calendar.time = Date(calendar.time.time - intervalMillis)
            point += interval
        }

        return columns
    }

    private fun intervalByMinutes(mode: ChartType): Int {
        return when (mode) {
            ChartType.DAILY -> 6 * 60                            // 6 hour
            ChartType.WEEKLY -> minsInDay * 2                    // 2 days
            ChartType.MONTHLY -> minsInDay * 6                   // 6 days
            ChartType.MONTHLY6 -> minsInDay * daysInMonth        // 1 month
            ChartType.MONTHLY18 -> minsInDay * daysInMonth * 2   // 2 month
        }
    }

    private fun minutesInMode(mode: ChartType): Int {
        return when (mode) {
            ChartType.DAILY -> minsInDay
            ChartType.WEEKLY -> minsInDay * 7
            ChartType.MONTHLY -> minsInDay * daysInMonth
            ChartType.MONTHLY6 -> minsInDay * daysInMonth * 6
            ChartType.MONTHLY18 -> minsInDay * daysInYear
        }
    }

    private fun pointName(calendar: Calendar, mode: ChartType): String {
        return when (mode) {
            ChartType.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            ChartType.WEEKLY -> calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
            ChartType.MONTHLY -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            ChartType.MONTHLY6 -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            ChartType.MONTHLY18 -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
        }
    }
}

package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine
import java.sql.Date
import java.util.*

class GridHelper(private val shape: RectF) {

    private val daysInMonth = 30
    private val daysInYear = 364
    private val minsInDay = 24 * 60

    fun setGridLines(priceTop: Float, priceStep: Float): List<GridLine> {
        var y: Float
        var value = priceTop
        val gridLines = mutableListOf<GridLine>()

        repeat(4) {
            val gridSpacing = shape.bottom / 4
            y = gridSpacing * it + shape.top

            var label = String.format("%.0f", value)
            if (value <= 1) {
                label = String.format("%.2f", value)
            }

            gridLines.add(GridLine(y, label))
            value -= priceStep
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
            Mode.DAILY -> {
            }
            Mode.WEEKLY,
            Mode.MONTHLY -> {
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                point += hours * 60
            }
            Mode.MONTHLY6,
            Mode.MONTHLY18 -> {
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

    private fun intervalByMinutes(mode: Mode): Int {
        return when (mode) {
            Mode.DAILY -> 6 * 60                            // 6 hour
            Mode.WEEKLY -> minsInDay * 2                    // 2 days
            Mode.MONTHLY -> minsInDay * 6                   // 6 days
            Mode.MONTHLY6 -> minsInDay * daysInMonth        // 1 month
            Mode.MONTHLY18 -> minsInDay * daysInMonth * 2   // 2 month
        }
    }

    private fun minutesInMode(mode: Mode): Int {
        return when (mode) {
            Mode.DAILY -> minsInDay
            Mode.WEEKLY -> minsInDay * 7
            Mode.MONTHLY -> minsInDay * daysInMonth
            Mode.MONTHLY6 -> minsInDay * daysInMonth * 6
            Mode.MONTHLY18 -> minsInDay * daysInYear
        }
    }

    private fun pointName(calendar: Calendar, mode: Mode): String {
        return when (mode) {
            Mode.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            Mode.WEEKLY -> calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
            Mode.MONTHLY -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            Mode.MONTHLY6 -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            Mode.MONTHLY18 -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
        }
    }
}

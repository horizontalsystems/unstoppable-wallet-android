package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine
import java.sql.Date
import java.util.*

class GridHelper(private val shape: RectF) {

    private val calendar = Calendar.getInstance()
    private val daysInMonth = calendar.getMaximum(Calendar.DAY_OF_MONTH)
    private val daysInYear = calendar.getMaximum(Calendar.DAY_OF_YEAR)
    private val minsInDay = 24 * 60

    fun setGridLines(priceTop: Float, priceStep: Float): List<GridLine> {
        var y: Float
        var value = priceTop
        val gridLines = mutableListOf<GridLine>()

        repeat(4) {
            val gridSpacing = shape.bottom / 4
            y = gridSpacing * it + shape.top

            gridLines.add(GridLine(y, String.format("%.0f", value)))
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

        val gridColumns = mutableListOf<GridColumn>()
        var point = calendar.get(Calendar.MINUTE)

        while (point <= minutes) {
            val offset = point * minutesPerPx
            if (shape.right - 10 > shape.right - offset) {
                gridColumns.add(GridColumn(shape.right - offset, pointName(calendar, data.mode)))
            }
            calendar.time = Date(calendar.time.time - intervalMillis)
            point += interval
        }

        return gridColumns
    }

    private fun intervalByMinutes(mode: Mode): Int {
        return when (mode) {
            Mode.DAILY -> 6 * 60                        // 6 hour
            Mode.WEEKLY -> minsInDay * 2                // 2 days
            Mode.MONTHLY -> minsInDay * 7               // 7 days
            Mode.MONTHLY6 -> minsInDay * daysInMonth    // 1 month
            Mode.ANNUAL -> minsInDay * daysInMonth * 3  // 3 month
        }
    }

    private fun minutesInMode(mode: Mode): Int {
        return when (mode) {
            Mode.DAILY -> minsInDay
            Mode.WEEKLY -> minsInDay * 7
            Mode.MONTHLY -> minsInDay * daysInMonth
            Mode.MONTHLY6 -> minsInDay * daysInMonth * 6
            Mode.ANNUAL -> minsInDay * daysInYear
        }
    }

    private fun pointName(calendar: Calendar, mode: Mode): String {
        return when (mode) {
            Mode.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            Mode.WEEKLY -> calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
            Mode.MONTHLY -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            Mode.MONTHLY6 -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            Mode.ANNUAL -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
        }
    }
}

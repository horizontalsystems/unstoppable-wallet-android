package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.lib.chartview.models.GridLine
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class GridHelper(private val shape: RectF, private val config: ChartConfig) {

    private val daysInMonth = 30
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

    fun setGridColumns(points: List<ChartPoint>, chartType: ChartType): List<GridColumn> {
        val startTimestamp = points.first().timestamp * 1000
        val endTimestamp = points.last().timestamp * 1000

        val date = Date(endTimestamp)
        val calendar = Calendar.getInstance().apply { time = date }

        val gridInterval = intervalMillis(chartType)
        //  We need to move last vertical grid line to nearest hour/day depending on chart type
        var gridOffset = calendar.get(Calendar.MINUTE)

        when (chartType) {
            ChartType.DAILY -> {
            }
            ChartType.WEEKLY,
            ChartType.MONTHLY -> {
                gridOffset += calendar.get(Calendar.HOUR_OF_DAY) * 60
            }
            ChartType.MONTHLY6,
            ChartType.MONTHLY18 -> {
                gridOffset += calendar.get(Calendar.HOUR_OF_DAY) * 60
                gridOffset += calendar.get(Calendar.DAY_OF_MONTH) * 24 * 60
            }
        }

        calendar.time = Date(date.time - gridOffset * 60 * 1000L)

        var xAxis = shape.right
        val delta = (endTimestamp - startTimestamp) / shape.width()
        val columns = mutableListOf<GridColumn>()

        while (xAxis >= 0) {
            val time = calendar.time.time
            xAxis = (time - startTimestamp) / delta

            columns.add(GridColumn(xAxis, pointName(calendar, chartType)))
            calendar.time = Date(time - gridInterval)
        }

        return columns
    }

    private fun intervalMillis(type: ChartType): Long {
        val interval = when (type) {
            ChartType.DAILY -> 6 * 60                            // 6 hour
            ChartType.WEEKLY -> minsInDay * 2                    // 2 days
            ChartType.MONTHLY -> minsInDay * 6                   // 6 days
            ChartType.MONTHLY6 -> minsInDay * daysInMonth        // 1 month
            ChartType.MONTHLY18 -> minsInDay * daysInMonth * 2   // 2 month
        }

        return interval * 60 * 1000L
    }

    private fun pointName(calendar: Calendar, type: ChartType): String {
        return when (type) {
            ChartType.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            ChartType.WEEKLY -> DateHelper.getShortDayOfWeek(calendar.time)
            ChartType.MONTHLY -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            ChartType.MONTHLY6 -> DateHelper.getShortMonth(calendar.time)
            ChartType.MONTHLY18 -> DateHelper.getShortMonth(calendar.time)
        }
    }
}

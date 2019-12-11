package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.GridColumn
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class GridHelper(private val shape: RectF, private val config: ChartConfig) {

    fun setGridColumns(chartType: ChartType, startTimestamp: Long, endTimestamp: Long): List<GridColumn> {
        val start = startTimestamp * 1000
        val end = endTimestamp * 1000

        val calendar = Calendar.getInstance().apply { time = Date() }
        var columnLabel = columnLabel(calendar, chartType)

        //  We need to move last vertical grid line to nearest hour/day depending on chart type
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        when (chartType) {
            ChartType.DAILY -> {
            }
            ChartType.WEEKLY,
            ChartType.MONTHLY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            ChartType.MONTHLY6,
            ChartType.MONTHLY12 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.DATE, 1)
            }
        }

        val delta = (end - start) / shape.right
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
            ChartType.DAILY -> calendar.add(Calendar.HOUR_OF_DAY, -6)       // 6 hour
            ChartType.WEEKLY -> calendar.add(Calendar.DAY_OF_WEEK, -2)      // 2 days
            ChartType.MONTHLY -> calendar.add(Calendar.DAY_OF_MONTH, -6)    // 6 days
            ChartType.MONTHLY6 -> calendar.add(Calendar.MONTH, -1)          // 1 month
            ChartType.MONTHLY12 -> calendar.add(Calendar.MONTH, -2)         // 2 month
        }
    }

    private fun columnLabel(calendar: Calendar, type: ChartType): String {
        return when (type) {
            ChartType.DAILY -> calendar.get(Calendar.HOUR_OF_DAY).toString()
            ChartType.WEEKLY -> DateHelper.formatDate(calendar.time, "EEE")
            ChartType.MONTHLY -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            ChartType.MONTHLY6 -> DateHelper.formatDate(calendar.time, "MMM")
            ChartType.MONTHLY12 -> DateHelper.formatDate(calendar.time, "MMM")
        }
    }
}

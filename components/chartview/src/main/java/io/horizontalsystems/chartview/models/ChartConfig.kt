package io.horizontalsystems.chartview.models

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.R
import java.math.BigDecimal

class ChartConfig(private val context: Context, attrs: AttributeSet?) {

    //  colors
    val textFont: Typeface = Typeface.DEFAULT
    var timelineTextColor = context.getColor(R.color.grey)
    var timelineTextSize = dp2px(10f)
    var timelineTextPadding = dp2px(4f)

    var gridTextColor = context.getColor(R.color.light_grey)
    var gridLineColor = context.getColor(R.color.steel_20)
    var gridDashColor = context.getColor(R.color.white_50)
    var gridLabelColor = context.getColor(R.color.grey_50)

    var gridTextSize = dp2px(12f)
    var gridTextPadding = dp2px(4f)
    var gridSideTextPadding = dp2px(16f)
    var gridEdgeOffset = dp2px(5f)

    var trendUpColor = context.getColor(R.color.green_d)
    var trendDownColor = context.getColor(R.color.red_d)
    var trendUpGradient = GradientColor(Color.parseColor("#0D416BFF"), Color.parseColor("#4D13D670"))
    var trendDownGradient = GradientColor(Color.parseColor("#0D7413D6"), Color.parseColor("#4DFF0303"))
    var pressedGradient = GradientColor(context.getColor(R.color.oz), context.getColor(R.color.oz))
    var outdatedGradient = GradientColor(context.getColor(R.color.grey_50), context.getColor(R.color.grey_50))

    var curveColor = trendUpColor
    var curveGradient = trendUpGradient
    var curvePressedColor = context.getColor(R.color.oz)
    var curveOutdatedColor = context.getColor(R.color.grey_50)
    var curveVerticalOffset = dp2px(18f)
    var curveMinimalVerticalOffset = dp2px(10f)
    var curveFastColor = Color.parseColor("#801A60FF")
    var curveSlowColor = Color.parseColor("#80ffa800")

    var curveDominanceLabelColor = context.getColor(R.color.jacob)

    var cursorColor = context.getColor(R.color.light)

    var volumeColor = context.getColor(R.color.steel_20)
    var volumeWidth = dp2px(4f)
    var volumeOffset = dp2px(8f)

    var macdHistogramUpColor = Color.parseColor("#8013D670")
    var macdHistogramDownColor = Color.parseColor("#80FF4820")
    var macdLineOffset = dp2px(2f)
    var macdHistogramOffset = dp2px(4f)

    var strokeWidth = dp2px(1f)
    var strokeDash = dp2px(2f)
    var strokeDashWidth = dp2px(0.5f)

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.Chart)
        try {
            timelineTextColor = ta.getInt(R.styleable.Chart_timelineTextColor, timelineTextColor)
            gridTextColor = ta.getInt(R.styleable.Chart_gridTextColor, gridTextColor)
            gridLineColor = ta.getInt(R.styleable.Chart_gridColor, gridLineColor)
            gridDashColor = ta.getInt(R.styleable.Chart_gridDashColor, gridDashColor)
            curveOutdatedColor = ta.getInt(R.styleable.Chart_partialChartColor, curveOutdatedColor)
            cursorColor = ta.getInt(R.styleable.Chart_cursorColor, cursorColor)
        } finally {
            ta.recycle()
        }
    }

    //  Helper methods
    fun setTrendColor(chartData: ChartData) {
        when {
            chartData.isExpired -> {
                curveColor = curveOutdatedColor
                curveGradient = outdatedGradient
            }
            chartData.diff() < BigDecimal.ZERO -> {
                curveColor = trendDownColor
                curveGradient = trendDownGradient
            }
            else -> {
                curveColor = trendUpColor
                curveGradient = trendUpGradient
            }
        }
    }

    private fun dp2px(dps: Float): Float {
        //  Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        //  Convert the dps to pixels, based on density scale
        return dps * scale + 0.5f
    }

    data class GradientColor(val startColor: Int, val endColor: Int)
}

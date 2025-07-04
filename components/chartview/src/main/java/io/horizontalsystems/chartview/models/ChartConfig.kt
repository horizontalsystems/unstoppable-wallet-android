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
    var timelineTextColor = context.getColor(R.color.leah)

    var gridTextColor = context.getColor(R.color.grey)
    var gridLineColor = context.getColor(R.color.steel_20)
    var gridDashColor = context.getColor(R.color.steel_10)
    var gridLabelColor = context.getColor(R.color.grey_50)

    var gridTextSize = dp2px(12f)
    var gridTextPadding = dp2px(4f)
    var gridSideTextPadding = dp2px(16f)
    var gridEdgeOffset = dp2px(5f)

    var trendUpColor = context.getColor(R.color.green_d)
    var trendDownColor = context.getColor(R.color.red_d)
    var neutralColor = context.getColor(R.color.jacob)
    var barColor = context.getColor(R.color.jacob)
    var barPressedColor = context.getColor(R.color.leah)
    var neutralGradientColor = GradientColor(Color.parseColor("#FFA800"), Color.parseColor("#FFA800"))
    var trendUpGradient = GradientColor(Color.parseColor("#416BFF"), Color.parseColor("#13D670"))
    var trendDownGradient = GradientColor(Color.parseColor("#7413D6"), Color.parseColor("#FF0303"))
    var pressedGradient = GradientColor(context.getColor(R.color.leah), context.getColor(R.color.leah))
    var outdatedGradient = GradientColor(context.getColor(R.color.grey_50), context.getColor(R.color.grey_50))

    var curveColor = trendUpColor
    var curveGradient = trendUpGradient
    var curvePressedColor = context.getColor(R.color.leah)
    var curveOutdatedColor = context.getColor(R.color.grey_50)
    var curveDisabledColor = context.getColor(R.color.grey)
    var curveVerticalOffset = dp2px(20f)
    var curveMinimalVerticalOffset = dp2px(10f)
    var curveFastColor = Color.parseColor("#801A60FF")
    var curveSlowColor = Color.parseColor("#80ffa800")

    var curveDominanceLabelColor = context.getColor(R.color.jacob)

    var cursorColor = context.getColor(R.color.leah)

    var volumeColor = context.getColor(R.color.steel_20)
    var volumeWidth = dp2px(2f)
    var volumeMinHeight = dp2px(2f)

    var strokeWidth = dp2px(1f)
    var strokeDash = dp2px(2f)
    var strokeDashWidth = dp2px(0.5f)

    var horizontalOffset = dp2px(8f)

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

    fun setTrendColor(chartData: ChartData) {
        when {
            chartData.disabled -> {
                curveColor = curveDisabledColor
                curveGradient = outdatedGradient
            }
            !chartData.isMovementChart -> {
                curveColor = neutralColor
                curveGradient = neutralGradientColor
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

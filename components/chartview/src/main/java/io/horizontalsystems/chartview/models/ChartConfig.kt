package io.horizontalsystems.chartview.models

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import io.horizontalsystems.chartview.R

class ChartConfig(private val context: Context, attrs: AttributeSet?) {

    //  colors
    var textFont = ResourcesCompat.getFont(context, R.font.noto_sans_medium)
    var curveColor = context.getColor(R.color.red_d)
    var touchColor = context.getColor(R.color.light)
    var gridColor = context.getColor(R.color.steel_20)
    var gridDottedColor = context.getColor(R.color.white_50)
    var textColor = context.getColor(R.color.grey)
    var textPriceColor = context.getColor(R.color.light_grey)
    var growColor = context.getColor(R.color.green_d)
    var fallColor = context.getColor(R.color.red_d)
    var cursorColor = context.getColor(R.color.light)
    var partialChartColor = context.getColor(R.color.grey_50)
    var volumeRectangleColor = context.getColor(R.color.steel_20)

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.Chart)
        try {
            growColor = ta.getInt(R.styleable.Chart_growColor, growColor)
            fallColor = ta.getInt(R.styleable.Chart_fallColor, fallColor)
            textColor = ta.getInt(R.styleable.Chart_textColor, textColor)
            textPriceColor = ta.getInt(R.styleable.Chart_textPriceColor, textPriceColor)
            gridColor = ta.getInt(R.styleable.Chart_gridColor, gridColor)
            touchColor = ta.getInt(R.styleable.Chart_touchColor, touchColor)
            cursorColor = ta.getInt(R.styleable.Chart_cursorColor, cursorColor)
            gridDottedColor = ta.getInt(R.styleable.Chart_gridDottedColor, gridDottedColor)
            partialChartColor = ta.getInt(R.styleable.Chart_partialChartColor, partialChartColor)
        } finally {
            ta.recycle()
        }
    }

    var textPricePT = dp2px(4f)
    var textPricePB = dp2px(8f)
    var textPricePL = dp2px(16f)
    var textPriceSize = dp2px(14f)
    var timelineTextSize = dp2px(12f)
    var timelineTextPadding = timelineTextSize + dp2px(2f)

    //  dimens
    var strokeWidth = 2f
    var strokeDotted = dp2px(2f)
    var strokeWidthDotted = 1F
    var curveVerticalOffset = dp2px(18f)
    var gridEdgeOffset = dp2px(5f)
    var volumeMaxHeightRatio = 0.8f // 40% of height
    var volumeBarWidth = dp2px(2f)

    //  Helper methods
    fun setTrendColour(startPoint: ChartPoint?, endPoint: ChartPoint?, endTimestamp: Long) {
        if (startPoint == null || endPoint == null) return
        if (endPoint.timestamp < endTimestamp) {
            curveColor = partialChartColor
        } else if (startPoint.value > endPoint.value) {
            curveColor = fallColor
        } else {
            curveColor = growColor
        }
    }

    fun xAxisPrice(x: Float, maxX: Float, text: String): Float {
        val width = measureTextWidth(text)
        if (width + x >= maxX) {
            return maxX - (width + textPricePT)
        }

        return x
    }

    fun yAxisPrice(y: Float, isTop: Boolean): Float {
        val textBoxOffset = 7
        if (isTop) {
            return y - textPricePB + textBoxOffset
        }

        return y + textPriceSize + textBoxOffset
    }

    private fun dp2px(dps: Float): Float {
        //  Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        //  Convert the dps to pixels, based on density scale
        return dps * scale + 0.5f
    }

    private fun measureTextWidth(text: String): Float {
        val paint = Paint()
        val width = paint.measureText(text)

        return dp2px(width)
    }
}

package io.horizontalsystems.bankwallet.lib.chartview.models

import android.content.Context
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import io.horizontalsystems.bankwallet.R

class ChartConfig(private val context: Context) {
    var showGrid = true
    var animated = true

    //  colors
    var curveColor = context.getColor(R.color.red_d)
    var touchColor = context.getColor(R.color.light)
    var gridColor = context.getColor(R.color.steel_20)
    var gridDottedColor = context.getColor(R.color.white_50)
    var textFont = ResourcesCompat.getFont(context, R.font.noto_sans_medium)
    var textColor = context.getColor(R.color.grey)
    var textPriceColor = context.getColor(R.color.light_grey)
    var growColor = context.getColor(R.color.green_d)
    var fallColor = context.getColor(R.color.red_d)
    var indicatorColor = context.getColor(R.color.light)
    var partialChartColor = context.getColor(R.color.grey_50)

    var textSize = dp2px(12f)
    var textPricePT = dp2px(4f)
    var textPricePB = dp2px(8f)
    var textPricePL = dp2px(16f)
    var textPriceSize = dp2px(14f)

    //  dimens
    var width = 0f
    var height = 0f
    var strokeWidth = 2f
    var strokeWidthDotted = 1F
    var offsetBottom = 0f
    var gridEdgeOffset = dp2px(5f)

    //  grid dimens
    var valueLow = 0f
    var valueTop = 0f
    var valueScale = 0

    //  Animation
    var animatedFraction = 0f

    //  Helper methods

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

    fun dp2px(dps: Float): Float {
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

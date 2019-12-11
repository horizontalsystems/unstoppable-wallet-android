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
    var textPadding = dp2px(4f)
    var textPriceSize = dp2px(14f)
    var textPricePadding = dp2px(16f)

    //  dimens
    var width = 0f
    var height = 0f
    var strokeWidth = 2f
    var strokeWidthDotted = 1F
    var offsetBottom = 0f
    var gridEdgeOffset = dp2px(5f)

    //  grid dimens
    var valueMin = 0f
    var valueMax = 0f
    var valueScale = 0

    //  Animation
    var animatedFraction = 0f

    //  Helper methods

    fun xAxisPrice(x: Float, maxX: Float, text: String): Float {
        val width = measureTextWidth(text)
        if (width + x >= maxX) {
            return maxX - (width + textPadding)
        }

        return x
    }

    fun yAxisPrice(y: Float, maxY: Float): Float {
        if (y + textPadding >= maxY - offsetBottom) {
            return y - (textPadding * 1.5f)
        }

        return y + textPriceSize + textPadding
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

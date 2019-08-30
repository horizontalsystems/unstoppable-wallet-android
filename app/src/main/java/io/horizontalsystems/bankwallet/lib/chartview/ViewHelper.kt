package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context
import android.graphics.Paint

class ViewHelper(private val context: Context) {
    fun dp2px(dps: Float): Float {
        //  Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        //  Convert the dps to pixels, based on density scale
        return dps * scale + 0.5f
    }

    fun measureWidth(value: Float, precision: Int): Float {
        return measureTextWidth(String.format("%.${precision}f", value))
    }

    private fun measureTextWidth(text: String): Float {
        val paint = Paint()
        val width = paint.measureText(text)

        return dp2px(width)
    }
}

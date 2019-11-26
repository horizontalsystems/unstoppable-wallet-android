package io.horizontalsystems.bankwallet.lib.chartview

import android.content.Context

class ViewHelper(private val context: Context) {
    fun dp2px(dps: Float): Float {
        //  Get the screen's density scale
        val scale = context.resources.displayMetrics.density
        //  Convert the dps to pixels, based on density scale
        return dps * scale + 0.5f
    }
}

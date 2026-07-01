package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context

object LayoutHelper {

    fun dp(dp: Float, context: Context?) = context?.let {
        val density = context.resources.displayMetrics.density
        if (dp == 0f) 0 else Math.ceil((density * dp).toDouble()).toInt()
    } ?: dp.toInt()

}

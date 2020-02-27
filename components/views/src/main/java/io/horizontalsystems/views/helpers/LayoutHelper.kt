package io.horizontalsystems.views.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat

object LayoutHelper {

    fun dp(dp: Float, context: Context?) = context?.let {
        val density = context.resources.displayMetrics.density
        if (dp == 0f) 0 else Math.ceil((density * dp).toDouble()).toInt()
    } ?: dp.toInt()

    fun getAttr(attr: Int, theme: Resources.Theme): Int? {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(attr, typedValue, true))
            typedValue.data
        else
            null
    }

    fun d(id: Int, context: Context): Drawable? {
        return ContextCompat.getDrawable(context, id)
    }

    fun getCoinDrawableResource(context: Context, coinCode: String): Int {
        val coinResourceName = "coin_${coinCode.replace("-", "_").toLowerCase()}"
        return context.resources.getIdentifier(coinResourceName, "drawable", context.packageName)
    }

    fun getCurrencyDrawableResource(context: Context, currencyCode: String): Int {
        return context.resources.getIdentifier("currency_$currencyCode", "drawable", context.packageName)
    }
}

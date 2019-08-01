package io.horizontalsystems.bankwallet.viewHelpers

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import io.horizontalsystems.bankwallet.core.App

object LayoutHelper {

    fun tintMenuIcons(menu: Menu, color: Int) {
        for (i in 0 until menu.size()) {
            val drawable = menu.getItem(i).icon
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

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

    fun getTintedDrawable(drawableResource: Int, colorId: Int, context: Context): Drawable? {
        val drawable = ContextCompat.getDrawable(context, drawableResource)?.let { DrawableCompat.wrap(it) }
        drawable?.mutate()
        val color = ContextCompat.getColor(context, colorId)
        drawable?.let { DrawableCompat.setTint(it, color) }
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }

    fun getCoinDrawableResource(coinCode: String): Int {
        val coinResourceName = "coin_${coinCode.replace("-", "_").toLowerCase()}"
        return App.instance.resources.getIdentifier(coinResourceName, "drawable", App.instance.packageName)
    }

    fun getLangDrawableResource(languageCode: String): Int {
        return App.instance.resources.getIdentifier("lang_$languageCode", "drawable", App.instance.packageName)
    }

    fun getCurrencyDrawableResource(currencyCode: String): Int {
        return App.instance.resources.getIdentifier("currency_$currencyCode", "drawable", App.instance.packageName)
    }

    fun getDeviceDensity(context: Context): String {
        val density = context.resources.displayMetrics.density
        return when {
            density >= 4.0 -> "xxxhdpi"
            density >= 3.0 -> "xxhdpi"
            density >= 2.0 -> "xhdpi"
            density >= 1.5 -> "hdpi"
            density >= 1.0 -> "mdpi"
            else -> "ldpi"
        }
    }
}

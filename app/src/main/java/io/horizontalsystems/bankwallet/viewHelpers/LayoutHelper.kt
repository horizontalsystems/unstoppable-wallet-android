package io.horizontalsystems.bankwallet.viewHelpers

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.TypedValue
import android.view.Menu
import io.horizontalsystems.bankwallet.R

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
        return when(coinCode) {
            "BCH" -> R.drawable.coin_bitcoin_cash
            "LTC" -> R.drawable.coin_litecoin
            "DASH" -> R.drawable.coin_dash
            "EOS" -> R.drawable.coin_eos
            "XMR" -> R.drawable.coin_monero
            "XRP" -> R.drawable.coin_xrp
            "ZEC" -> R.drawable.coin_zcash
            "ETH" -> R.drawable.coin_ether
            "ADA" -> R.drawable.coin_cardano
            "XLM" -> R.drawable.coin_stellar
            "USDT" -> R.drawable.coin_tether
            else -> R.drawable.coin_bitcoin
        }
    }

    fun getInfoBadge(wordListBackedUp: Boolean, resources: Resources): Drawable? {
        var infoBadge: Drawable? = null
        if (!wordListBackedUp) {
            infoBadge = resources.getDrawable(R.drawable.info, null)
            infoBadge?.setTint(resources.getColor(R.color.red_warning, null))
        }
        return infoBadge
    }

}

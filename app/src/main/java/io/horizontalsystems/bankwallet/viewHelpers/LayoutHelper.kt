package io.horizontalsystems.bankwallet.viewHelpers

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
        return when (coinCode) {
            "BCH" -> R.drawable.coin_bch
            "LTC" -> R.drawable.coin_ltc
            "DASH" -> R.drawable.coin_dash
            "EOS" -> R.drawable.coin_eos
            "XMR" -> R.drawable.coin_xmr
            "XRP" -> R.drawable.coin_xrp
            "ZEC" -> R.drawable.coin_zec
            "ETH" -> R.drawable.coin_eth
            "ADA" -> R.drawable.coin_ada
            "XLM" -> R.drawable.coin_xlm
            "USDT" -> R.drawable.coin_usdt
            "BAT" -> R.drawable.coin_bat
            "AURA" -> R.drawable.coin_aura
            "BNB" -> R.drawable.coin_bnb
            "BNT" -> R.drawable.coin_bnt
            "DAI" -> R.drawable.coin_dai
            "DAT" -> R.drawable.coin_dat
            "DGD" -> R.drawable.coin_dgd
            "DGX" -> R.drawable.coin_dgx
            "ENJ" -> R.drawable.coin_enj
            "EURS" -> R.drawable.coin_eurs
            "GNT" -> R.drawable.coin_gnt
            "GUSD" -> R.drawable.coin_gusd
            "HT" -> R.drawable.coin_ht
            "IDXM" -> R.drawable.coin_idxm
            "KCS" -> R.drawable.coin_kcs
            "KNC" -> R.drawable.coin_knc
            "LINK" -> R.drawable.coin_link
            "LOOM" -> R.drawable.coin_loom
            "MANA" -> R.drawable.coin_mana
            "MCO" -> R.drawable.coin_mco
            "MITH" -> R.drawable.coin_mith
            "MKR" -> R.drawable.coin_mkr
            "NEXO" -> R.drawable.coin_nexo
            "NPXS" -> R.drawable.coin_npxs
            "OMG" -> R.drawable.coin_omg
            "PAX" -> R.drawable.coin_pax
            "POLY" -> R.drawable.coin_poly
            "PPT" -> R.drawable.coin_ppt
            "R" -> R.drawable.coin_r
            "REP" -> R.drawable.coin_rep
            "SNT" -> R.drawable.coin_snt
            "TUSD" -> R.drawable.coin_tusd
            "USDC" -> R.drawable.coin_usdc
            "WTC" -> R.drawable.coin_wtc
            "WAX" -> R.drawable.coin_wax
            "ZIL" -> R.drawable.coin_zil
            "ZRX" -> R.drawable.coin_zrx
            "ELF" -> R.drawable.coin_elf
            "CRO" -> R.drawable.coin_cro
            "HOT" -> R.drawable.coin_hot
            "IOST" -> R.drawable.coin_iost
            "LRC" -> R.drawable.coin_lrc
            "ORBS" -> R.drawable.coin_orbs
            else -> R.drawable.coin_btc
        }
    }

    fun getInfoBadge(wordListBackedUp: Boolean, resources: Resources): Drawable? {
        var infoBadge: Drawable? = null
        if (!wordListBackedUp) {
            infoBadge = resources.getDrawable(R.drawable.ic_info, null)
            infoBadge?.setTint(resources.getColor(R.color.red_warning, null))
        }
        return infoBadge
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

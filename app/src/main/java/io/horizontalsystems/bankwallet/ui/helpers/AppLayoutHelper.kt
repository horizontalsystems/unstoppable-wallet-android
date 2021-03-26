package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.R
import java.util.*

object AppLayoutHelper {
    fun getCoinDrawable(context: Context, coinType: CoinType): Drawable? {
        val coinDrawableResId = getCoinDrawableResId(context, coinType)

        val resId = when {
            coinDrawableResId != null -> coinDrawableResId
            coinType is CoinType.Erc20 -> R.drawable.ic_erc20
            coinType is CoinType.Bep2 -> R.drawable.ic_bep2
            coinType is CoinType.Bep20 -> R.drawable.ic_bep20
            else -> null
        }

        return resId?.let { ContextCompat.getDrawable(context, it) }
    }

    fun getCoinDrawableResId(context: Context, coinType: CoinType): Int? {
        val coinResourceName = coinType.ID.replace("[|-]".toRegex(), "_").toLowerCase(Locale.ENGLISH)
        val imgRes = context.resources.getIdentifier(coinResourceName, "drawable", context.packageName)

        return when {
            imgRes != 0 -> imgRes
            else -> null
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}
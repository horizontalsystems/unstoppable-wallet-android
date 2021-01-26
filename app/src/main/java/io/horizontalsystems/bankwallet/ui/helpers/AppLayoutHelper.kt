package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.views.R

object AppLayoutHelper {
    fun getCoinDrawable(context: Context, coinCode: String, coinType: CoinType? = null): Drawable? {
        val coinDrawableResId = getCoinDrawableResId(context, coinCode)

        val resId = when {
            coinDrawableResId != null -> coinDrawableResId
            coinType is CoinType.Erc20 -> R.drawable.ic_erc20
            coinType is CoinType.Binance -> R.drawable.ic_bep2
            else -> null
        }

        return resId?.let { ContextCompat.getDrawable(context, it) }
    }

    fun getCoinDrawableResId(context: Context, coinCode: String): Int? {
        val coinResourceName = "coin_${coinCode.replace("-", "_").toLowerCase()}"
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
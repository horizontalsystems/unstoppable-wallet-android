package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.views.R

object AppLayoutHelper {
    fun getCoinDrawable(context: Context, coinCode: String, coinType: CoinType? = null): Drawable? {
        val coinResourceName = "coin_${coinCode.replace("-", "_").toLowerCase()}"
        val imgRes = context.resources.getIdentifier(coinResourceName, "drawable", context.packageName)

        try {
            return ContextCompat.getDrawable(context, imgRes)
        } catch (e: Resources.NotFoundException) {
            //icon resource not existing
        }

        if (coinType is CoinType.Erc20) {
            return ContextCompat.getDrawable(context, R.drawable.ic_erc20)
        }

        return null
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
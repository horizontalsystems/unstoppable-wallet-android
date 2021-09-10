package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.R
import java.util.*

object AppLayoutHelper {
    fun getCoinDrawable(context: Context, coinType: CoinType): Drawable? {
        return ContextCompat.getDrawable(context, getCoinDrawableOrDefaultResId(context, coinType))
    }

    fun getCoinDrawableOrDefaultResId(context: Context, coinType: CoinType): Int {
        val coinDrawableResId = getCoinDrawableResId(context, coinType)

        val resId = when {
            coinDrawableResId != null -> coinDrawableResId
            coinType is CoinType.Erc20 -> R.drawable.erc20
            coinType is CoinType.Bep2 -> R.drawable.bep2
            coinType is CoinType.Bep20 -> R.drawable.bep20
            else -> R.drawable.place_holder
        }
        return resId
    }

    private fun getCoinDrawableResId(context: Context, coinType: CoinType): Int? {
        val coinResourceName = coinType.ID.replace("[|-]".toRegex(), "_").toLowerCase(Locale.ENGLISH)
        val imgRes = context.resources.getIdentifier(coinResourceName, "drawable", context.packageName)

        return when {
            imgRes != 0 -> imgRes
            else -> null
        }
    }
}

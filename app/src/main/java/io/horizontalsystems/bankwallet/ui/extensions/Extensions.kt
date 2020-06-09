package io.horizontalsystems.bankwallet.ui.extensions

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.R

fun ImageView.setCoinImage(coinCode: String, coinType: CoinType? = null) {
    setImageDrawable(AppLayoutHelper.getCoinDrawable(context, coinCode, coinType))

    val greyColor = ContextCompat.getColor(context, R.color.grey)
    val tintColorStateList = ColorStateList.valueOf(greyColor)
    imageTintList = tintColorStateList
}
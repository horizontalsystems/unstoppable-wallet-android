package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.view_coin_icon.view.*

class CoinIconView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_coin_icon, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(coin: Coin) {
        dynamicCoinIcon.setImageResource(LayoutHelper.getCoinDrawableResource(coin.code))
    }

    fun bind(coinCode: String) {
        dynamicCoinIcon.setImageResource(LayoutHelper.getCoinDrawableResource(coinCode))
    }

    fun bind(icon: Int) {
        dynamicCoinIcon.setImageResource(icon)
    }
}

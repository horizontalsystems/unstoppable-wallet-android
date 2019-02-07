package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import kotlinx.android.synthetic.main.view_coin_icon.view.*

class CoinIconView : ConstraintLayout {
    private val ipfsUrl = App.appConfigProvider.ipfsUrl

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    fun bind(coin: Coin) {
        when (coin.type) {
            is CoinType.Erc20 -> {
                val density = LayoutHelper.getDeviceDensity(context)

                dynamicCoinIcon.hierarchy.setPlaceholderImage(R.drawable.phi)
                dynamicCoinIcon.setImageURI("${ipfsUrl}blockchain/ETH/erc20/${coin.type.address}/icons/android/drawable-$density/icon.png")
            }
            else -> {
                dynamicCoinIcon.setActualImageResource(LayoutHelper.getCoinDrawableResource(
                        TextHelper.getCleanCoinCode(coin.code)
                ))
            }
        }
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_coin_icon, this)
    }
}

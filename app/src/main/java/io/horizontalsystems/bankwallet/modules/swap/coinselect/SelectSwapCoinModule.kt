package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.entities.Coin

object SelectSwapCoinModule {

    const val selectedCoinKey = "selectedCoinKey"

    fun start(context: Context, selectedCoin: Coin?) {
        val intent = Intent(context, SelectSwapCoinActivity::class.java)
        intent.putExtra(selectedCoinKey, selectedCoin)

        context.startActivity(intent)
    }

}

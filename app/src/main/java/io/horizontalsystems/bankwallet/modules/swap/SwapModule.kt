package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.entities.Wallet

object SwapModule {

    const val swapWalletKey = "swapWalletKey"

    fun start(context: Context, wallet: Wallet) {
        val intent = Intent(context, SwapActivity::class.java)
        intent.putExtra(swapWalletKey, wallet)

        context.startActivity(intent)
    }

}

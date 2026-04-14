package com.quantum.wallet.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.FullCoin

class ReceiveSharedViewModel : ViewModel() {

    var coinUid: String? = null
    var wallet: Wallet? = null

    val activeAccount: Account?
        get() = App.accountManager.activeAccount

    fun fullCoin(): FullCoin? {
        val coinUid = coinUid ?: return null
        return App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

}
package io.horizontalsystems.walletkit.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.marketkit.models.FullCoin

class ReceiveSharedViewModel : ViewModel() {

    var coinUid: String? = null

    val activeAccount: Account?
        get() = App.accountManager.activeAccount

    fun fullCoin(): FullCoin? {
        val coinUid = coinUid ?: return null
        return App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

}
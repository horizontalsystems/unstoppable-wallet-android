package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
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
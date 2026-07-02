package io.horizontalsystems.core.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.entities.Account
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
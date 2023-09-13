package io.horizontalsystems.bankwallet.modules.receivemain

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App

class NetworkSelectInitViewModel(coinUid: String) : ViewModel() {
    val fullCoin = App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    val activeAccount = App.accountManager.activeAccount
}

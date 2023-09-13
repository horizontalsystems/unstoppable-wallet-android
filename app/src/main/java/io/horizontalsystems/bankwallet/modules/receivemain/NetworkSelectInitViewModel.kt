package cash.p.terminal.modules.receivemain

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App

class NetworkSelectInitViewModel(coinUid: String) : ViewModel() {
    val fullCoin = App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    val activeAccount = App.accountManager.activeAccount
}

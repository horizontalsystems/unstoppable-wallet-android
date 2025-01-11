package cash.p.terminal.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App
import cash.p.terminal.modules.receive.ui.UsedAddressesParams
import cash.p.terminal.wallet.entities.FullCoin

class ReceiveSharedViewModel : ViewModel() {

    var wallet: cash.p.terminal.wallet.Wallet? = null
    var coinUid: String? = null
    var usedAddressesParams: UsedAddressesParams? = null

    val activeAccount: cash.p.terminal.wallet.Account?
        get() = App.accountManager.activeAccount

    fun fullCoin(): FullCoin? {
        val coinUid = coinUid ?: return null
        return App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

}
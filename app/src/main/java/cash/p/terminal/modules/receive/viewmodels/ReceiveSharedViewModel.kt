package cash.p.terminal.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.receive.ui.UsedAddressesParams
import io.horizontalsystems.marketkit.models.FullCoin

class ReceiveSharedViewModel : ViewModel() {

    var wallet: Wallet? = null
    var coinUid: String? = null
    var usedAddressesParams: UsedAddressesParams? = null

    val activeAccount: Account?
        get() = App.accountManager.activeAccount

    fun fullCoin(): FullCoin? {
        val coinUid = coinUid ?: return null
        return App.marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

}
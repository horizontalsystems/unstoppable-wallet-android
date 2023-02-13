package cash.p.terminal.modules.receive

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.entities.Wallet
import cash.p.terminal.entities.addressType

class ReceiveViewModel(
    val wallet: Wallet,
    receiveAdapter: IReceiveAdapter
) : ViewModel() {

    val receiveAddress  = receiveAdapter.receiveAddress
    val addressType = wallet.coinSettings.derivation?.addressType
    val watchAccount = wallet.account.isWatchAccount
}

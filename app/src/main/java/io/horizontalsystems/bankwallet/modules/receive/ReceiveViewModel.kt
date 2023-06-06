package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.entities.Wallet

class ReceiveViewModel(
    val wallet: Wallet,
    receiveAdapter: IReceiveAdapter
) : ViewModel() {

    val receiveAddress  = receiveAdapter.receiveAddress
    val addressType = wallet.coinSettings.derivation?.addressType
    val watchAccount = wallet.account.isWatchAccount
    val isAccountActive = receiveAdapter.isAccountActive
}

package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.addressType

class ReceiveViewModel(
    val wallet: Wallet,
    receiveAdapter: IReceiveAdapter
) : ViewModel() {

    val receiveAddress  = receiveAdapter.receiveAddress
    val addressType = wallet.coinSettings.derivation?.addressType
    val testNet = !receiveAdapter.isMainnet
    val watchAccount = wallet.account.isWatchAccount
}

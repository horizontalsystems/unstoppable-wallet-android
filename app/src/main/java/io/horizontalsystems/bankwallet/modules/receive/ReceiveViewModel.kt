package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.addressType

class ReceiveViewModel(wallet: Wallet, adapterManager: IAdapterManager) : ViewModel() {

    val receiveAddress: String
    val addressType: String?

    init {
        val receiveAdapter = adapterManager.getReceiveAdapterForWallet(wallet) ?: throw NoReceiverAdapter()

        receiveAddress = receiveAdapter.receiveAddress
        addressType = wallet.configuredCoin.settings.derivation?.addressType()
    }

    class NoReceiverAdapter : Error("No Receiver Adapter")

}

package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.addressType
import io.horizontalsystems.bankwallet.modules.balance.NetworkTypeChecker

class ReceiveViewModel(
        wallet: Wallet,
        adapterManager: IAdapterManager,
        networkTypeChecker: NetworkTypeChecker) : ViewModel() {

    val receiveAddress: String
    val addressType: String?
    val testNet: Boolean

    init {
        val receiveAdapter = adapterManager.getReceiveAdapterForWallet(wallet) ?: throw NoReceiverAdapter()

        testNet = !networkTypeChecker.isMainNet(wallet)
        receiveAddress = receiveAdapter.receiveAddress
        addressType = wallet.coinSettings.derivation?.addressType
    }

    class NoReceiverAdapter : Error("No Receiver Adapter")

}

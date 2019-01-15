package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class ReceiveInteractor(private var coinCode: CoinCode?, private var walletManager: IWalletManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        val addresses = walletManager.wallets.filter { coinCode == null || it.coinCode == coinCode }.map {
            AddressItem(it.adapter.receiveAddress, it.coinCode, it.title)
        }

        if (addresses.isNotEmpty()) {
            delegate?.didReceiveAddresses(addresses)
        }
    }

}

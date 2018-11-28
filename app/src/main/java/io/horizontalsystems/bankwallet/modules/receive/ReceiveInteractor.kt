package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.Coin

class ReceiveInteractor(private var coin: Coin?, private var walletManager: IWalletManager, private var clipboardManager: IClipboardManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        val addresses = walletManager.wallets.filter { coin == null || it.coin == coin }.map {
            AddressItem(it.adapter.receiveAddress, it.coin)
        }

        if (addresses.isNotEmpty()) {
            delegate?.didReceiveAddresses(addresses)
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }

}

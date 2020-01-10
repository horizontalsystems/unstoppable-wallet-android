package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem

class ReceiveInteractor(
        private var wallet: Wallet,
        private var adapterManager: IAdapterManager,
        private var clipboardManager: IClipboardManager
) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        adapterManager.getReceiveAdapterForWallet(wallet)?.let { adapter ->
            val addressItem = AddressItem(adapter.receiveAddress,
                                          adapter.getReceiveAddressType(wallet), wallet.coin)
            delegate?.didReceiveAddress(addressItem)
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }
}

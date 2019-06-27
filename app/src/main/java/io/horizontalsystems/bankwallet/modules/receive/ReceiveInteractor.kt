package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class ReceiveInteractor(
        private var coinCode: CoinCode?,
        private var adapterManager: IAdapterManager,
        private var clipboardManager: IClipboardManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        adapterManager.adapters.firstOrNull { it.wallet.coin.code == coinCode }?.let {
            val addressItem = AddressItem(it.receiveAddress, it.wallet.coin)
            delegate?.didReceiveAddress(addressItem)
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }
}

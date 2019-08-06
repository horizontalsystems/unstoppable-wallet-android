package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class ReceiveInteractor(
        private var coinCode: CoinCode?,
        private val walletManager: IWalletManager,
        private var adapterManager: IAdapterManager,
        private var clipboardManager: IClipboardManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress() {
        walletManager.wallets.firstOrNull { it.coin.code == coinCode }?.let { wallet ->
            adapterManager.getAdapterForWallet(wallet)?.let { adapter ->
                val addressItem = AddressItem(adapter.receiveAddress, wallet.coin)
                delegate?.didReceiveAddress(addressItem)
            }
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }
}

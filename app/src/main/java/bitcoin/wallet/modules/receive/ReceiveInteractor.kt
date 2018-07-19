package bitcoin.wallet.modules.receive

import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IClipboardManager

class ReceiveInteractor(private var blockchainManager: BlockchainManager, private var clipboardManager: IClipboardManager) : ReceiveModule.IInteractor {

    var delegate: ReceiveModule.IInteractorDelegate? = null

    override fun getReceiveAddress(coinCode: String) {
        try {
            val address = blockchainManager.getReceiveAddress(coinCode)
            delegate?.didReceiveAddress(address)
        } catch (e: Exception) {
            delegate?.didFailToReceiveAddress(e)
        }
    }

    override fun copyToClipboard(coinAddress: String) {
        clipboardManager.copyText(coinAddress)
        delegate?.didCopyToClipboard()
    }

}

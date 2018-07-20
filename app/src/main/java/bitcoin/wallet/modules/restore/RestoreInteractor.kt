package bitcoin.wallet.modules.restore

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IMnemonic

class RestoreInteractor(private val mnemonic: IMnemonic, private val blockchainManager: BlockchainManager) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {
        if (mnemonic.validateWords(words)) {
            try {
                blockchainManager.initNewWallet(words)
                delegate?.didRestore()
            } catch (e: UserNotAuthenticatedException) {
                delegate?.didFailToRestore(e)
            }
        } else {
            delegate?.didFailToRestore(Exception())
        }
    }
}

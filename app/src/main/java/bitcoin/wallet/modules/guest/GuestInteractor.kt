package bitcoin.wallet.modules.guest

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IMnemonic

class GuestInteractor(private val mnemonic: IMnemonic, private val blockchainManager: BlockchainManager) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        try {
            blockchainManager.initNewWallet(mnemonic.generateWords())
            delegate?.didCreateWallet()
        } catch (e: UserNotAuthenticatedException) {
            delegate?.didFailToCreateWallet(e)
        }
    }

}

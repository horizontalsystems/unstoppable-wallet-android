package bitcoin.wallet.modules.guest

import android.security.keystore.UserNotAuthenticatedException

class GuestInteractor() : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        try {
//            blockchainManager.initNewWallet(mnemonic.generateWords())
            delegate?.didCreateWallet()
        } catch (e: UserNotAuthenticatedException) {
            delegate?.didFailToCreateWallet(e)
        }
    }

}

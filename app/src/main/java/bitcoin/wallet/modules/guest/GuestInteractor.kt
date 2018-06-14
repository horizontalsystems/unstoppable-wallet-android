package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.IMnemonic

class GuestInteractor(private val mnemonic: IMnemonic, private val localStorage: ILocalStorage) : GuestModule.IInteractor {

    lateinit var delegate: GuestModule.IInteractorDelegate

    override fun createWallet() {
        localStorage.saveWords(mnemonic.generateWords())
        delegate.didCreateWallet()
    }

}

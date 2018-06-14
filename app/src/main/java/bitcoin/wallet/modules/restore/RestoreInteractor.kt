package bitcoin.wallet.modules.restore

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.IMnemonic

class RestoreInteractor(private val mnemonic: IMnemonic, private val localStorage: ILocalStorage) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {
        if (mnemonic.validateWords(words)) {
            localStorage.saveWords(words)
            delegate?.didRestore()
        } else {
            delegate?.didFailToRestore()
        }
    }

}

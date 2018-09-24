package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.WordsManager

class GuestInteractor(private val wordsManager: WordsManager, private val adapterManager: AdapterManager) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        val words = wordsManager.createWords()
        adapterManager.initAdapters(words)
        delegate?.didCreateWallet()
    }

}

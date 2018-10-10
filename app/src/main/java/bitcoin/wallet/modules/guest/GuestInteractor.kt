package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.WordsManager

class GuestInteractor(private val wordsManager: WordsManager, private val adapterManager: AdapterManager, private val keystoreSafeExecute: GuestModule.IKeyStoreSafeExecute) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    val words = wordsManager.createWords()
                    adapterManager.initAdapters(words)
                },
                onSuccess = Runnable { delegate?.didCreateWallet() },
                onFailure = Runnable { delegate?.didFailToCreateWallet() }
        )
    }

}

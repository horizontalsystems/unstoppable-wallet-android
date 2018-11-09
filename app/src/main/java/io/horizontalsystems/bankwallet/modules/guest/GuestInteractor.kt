package io.horizontalsystems.bankwallet.modules.guest

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.managers.WordsManager

class GuestInteractor(
        private val wordsManager: WordsManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    wordsManager.createWords()
                },
                onSuccess = Runnable { delegate?.didCreateWallet() },
                onFailure = Runnable { delegate?.didFailToCreateWallet() }
        )
    }

}

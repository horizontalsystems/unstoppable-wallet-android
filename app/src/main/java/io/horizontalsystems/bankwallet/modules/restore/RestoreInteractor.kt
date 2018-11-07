package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.managers.WordsManager

class RestoreInteractor(
        private val wordsManager: WordsManager,
        private val adapterManager: IAdapterManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {
        keystoreSafeExecute.safeExecute(
                action = Runnable { wordsManager.restore(words) },
                onSuccess = Runnable {
                    adapterManager.start()
                    wordsManager.isBackedUp = true
                    delegate?.didRestore()
                },
                onFailure = Runnable { delegate?.didFailToRestore() }
        )
    }
}

package io.horizontalsystems.bankwallet.modules.pin

import io.horizontalsystems.bankwallet.core.*

class PinInteractor(
        private val pinManager: IPinManager,
        private val wordsManager: IWordsManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        private val localStorage: ILocalStorage) : PinModule.IPinInteractor {

    var delegate: PinModule.IPinInteractorDelegate? = null
    private var storedPin: String? = null

    override fun set(pin: String?) {
        storedPin = pin
    }

    override fun validate(pin: String): Boolean {
        return storedPin == pin
    }

    override fun save(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    pinManager.store(pin)
                },
                onSuccess = Runnable { delegate?.didSavePin() },
                onFailure = Runnable { delegate?.didFailToSavePin() }
        )
    }

    override fun unlock(pin: String): Boolean {
        return pinManager.validate(pin)
    }

    override fun startAdapters() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    wordsManager.safeLoad()
                    wordsManager.loggedInSubject.onNext(if (localStorage.isNewWallet) LogInState.CREATE else LogInState.RESTORE)
                },
                onSuccess = Runnable { delegate?.didStartedAdapters() }
        )
    }
}

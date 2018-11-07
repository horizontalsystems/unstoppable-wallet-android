package io.horizontalsystems.bankwallet.modules.pin

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.IPinManager

class PinInteractor(
        private val pinManager: IPinManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinModule.IPinInteractor {

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
}

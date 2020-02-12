package io.horizontalsystems.pin

import io.horizontalsystems.core.IPinManager

class PinInteractor(private val pinManager: IPinManager) : PinModule.IInteractor {

    var delegate: PinModule.IInteractorDelegate? = null
    private var storedPin: String? = null

    override fun set(pin: String?) {
        storedPin = pin
    }

    override fun validate(pin: String): Boolean {
        return storedPin == pin
    }

    override fun save(pin: String) {
        try {
            pinManager.store(pin)
            delegate?.didSavePin()
        } catch (ex: Exception) {
            delegate?.didFailToSavePin()
        }
    }

    override fun unlock(pin: String): Boolean {
        return pinManager.validate(pin)
    }

}

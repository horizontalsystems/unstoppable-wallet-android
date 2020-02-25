package io.horizontalsystems.pin.core

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.core.IPinManager
import io.horizontalsystems.core.ISecuredStorage

class PinManager(private val securedStorage: ISecuredStorage) : IPinManager {

    override val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    override var isFingerprintEnabled: Boolean
        get() = securedStorage.isFingerprintEnabled
        set(value) {
            securedStorage.isFingerprintEnabled = value
        }

    @Throws(UserNotAuthenticatedException::class)
    override fun store(pin: String) {
        securedStorage.savePin(pin)
    }

    override fun validate(pin: String): Boolean {
        return securedStorage.savedPin == pin
    }

    override fun clear() {
        securedStorage.removePin()
        securedStorage.isFingerprintEnabled = false
    }
}

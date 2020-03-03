package io.horizontalsystems.pin.core

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.core.ISecuredStorage

class PinManager(private val securedStorage: ISecuredStorage) {

    val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    var isFingerprintEnabled: Boolean
        get() = securedStorage.isFingerprintEnabled
        set(value) {
            securedStorage.isFingerprintEnabled = value
        }

    @Throws(UserNotAuthenticatedException::class)
    fun store(pin: String) {
        securedStorage.savePin(pin)
    }

    fun validate(pin: String): Boolean {
        return securedStorage.savedPin == pin
    }

    fun clear() {
        securedStorage.removePin()
        securedStorage.isFingerprintEnabled = false
    }
}

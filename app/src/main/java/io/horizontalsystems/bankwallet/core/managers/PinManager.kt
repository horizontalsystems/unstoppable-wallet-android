package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.core.IPinManager

class PinManager(private val securedStorage: ISecuredStorage, val localStorage: ILocalStorage) : IPinManager {

    override val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    override var isFingerprintEnabled: Boolean
        get() = localStorage.isFingerprintEnabled
        set(value) {
            localStorage.isFingerprintEnabled = value
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
        localStorage.isFingerprintEnabled = false
    }

}

package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.IPinManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage

class PinManager(private val securedStorage: ISecuredStorage) : IPinManager {

    override val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    @Throws(UserNotAuthenticatedException::class)
    override fun store(pin: String) {
        securedStorage.savePin(pin)
    }

    override fun validate(pin: String): Boolean {
        return securedStorage.savedPin == pin
    }

    override fun clear() {
        securedStorage.removePin()
    }

}

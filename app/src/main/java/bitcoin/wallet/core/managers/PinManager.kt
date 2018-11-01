package bitcoin.wallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.core.IPinManager
import bitcoin.wallet.core.ISecuredStorage

class PinManager(private val securedStorage: ISecuredStorage): IPinManager {

    override val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    @Throws(UserNotAuthenticatedException::class)
    override fun store(pin: String) {
        securedStorage.savePin(pin)
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun validate(pin: String): Boolean {
        return securedStorage.savedPin == pin
    }
}

package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import android.app.KeyguardManager
import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IPinManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage

class PinManager(private val securedStorage: ISecuredStorage) : IPinManager {

    override val isDeviceLockEnabled: Boolean
        get() {
            val keyguardManager = App.instance.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
            return keyguardManager.isKeyguardSecure
        }

    override var pin: String? = null

    override fun safeLoad() {
        pin = securedStorage.savedPin
    }

    override val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    @Throws(UserNotAuthenticatedException::class)
    override fun store(pin: String) {
        securedStorage.savePin(pin)
        this.pin = pin
    }

    override fun validate(pin: String): Boolean {
        return this.pin == pin
    }

    override fun clear() {
        pin = null
        securedStorage.removePin()
    }
}

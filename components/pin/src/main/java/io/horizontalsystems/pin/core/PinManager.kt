package io.horizontalsystems.pin.core

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.core.ISecuredStorage
import io.reactivex.subjects.PublishSubject

class PinManager(private val securedStorage: ISecuredStorage) {

    val pinSetSubject = PublishSubject.create<Unit>()

    val isPinSet: Boolean
        get() = !securedStorage.pinIsEmpty()

    var isBiometricAuthEnabled: Boolean
        get() = securedStorage.isBiometricAuthEnabled
        set(value) {
            securedStorage.isBiometricAuthEnabled = value
        }

    @Throws(UserNotAuthenticatedException::class)
    fun store(pin: String) {
        securedStorage.savePin(pin)
        pinSetSubject.onNext(Unit)
    }

    fun validate(pin: String): Boolean {
        return securedStorage.savedPin == pin
    }

    fun clear() {
        securedStorage.removePin()
        securedStorage.isBiometricAuthEnabled = false
        pinSetSubject.onNext(Unit)
    }
}

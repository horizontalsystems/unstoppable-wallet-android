package io.horizontalsystems.pin.core

import android.text.TextUtils
import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IPinStorage
import io.reactivex.subjects.PublishSubject

class PinManager(
        private val encryptionManager: IEncryptionManager,
        private val pinStorage: IPinStorage) {

    val pinSetSubject = PublishSubject.create<Unit>()

    val isPinSet: Boolean
        get() = !pinStorage.pin.isNullOrEmpty()

    private val savedPin: String?
        get() {
            val string = pinStorage.pin ?: return null
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string)
            }
        }

    @Throws
    fun store(pin: String) {
        pinStorage.pin = encryptionManager.encrypt(pin)
        pinSetSubject.onNext(Unit)
    }

    @Throws
    fun validate(pin: String): Boolean {
        return savedPin == pin
    }

    fun clear() {
        pinStorage.clearPin()
        pinStorage.biometricAuthEnabled = false
        pinSetSubject.onNext(Unit)
    }

}
package io.horizontalsystems.bankwallet.core.managers

import android.text.TextUtils
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.core.IEncryptionManager

class SecuredStorageManager(private val encryptionManager: IEncryptionManager) : ISecuredStorage {

    private val lockPin = "lock_pin"

    override val savedPin: String?
        get() {
            val string = App.preferences.getString(lockPin, null) ?: return null
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string)
            }
        }

    override fun savePin(pin: String) {
        App.preferences.edit().putString(lockPin, encryptionManager.encrypt(pin)).apply()
    }

    override fun removePin() {
        App.preferences.edit().remove(lockPin).apply()
    }

    override fun pinIsEmpty(): Boolean {
        val string = App.preferences.getString(lockPin, null)
        return string.isNullOrEmpty()
    }

}

package bitcoin.wallet.core.managers

import android.text.TextUtils
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IEncryptionManager
import bitcoin.wallet.core.ILocalStorage

class PreferencesManager(private val encryptionManager: IEncryptionManager) : ILocalStorage {

    override val savedWords: List<String>?
        get() {
            val string = App.preferences.getString("mnemonicWords", null)
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string).split(" ").filter { it.isNotBlank() }
            }
        }

    override fun saveWords(words: List<String>) {
        App.preferences.edit().putString("mnemonicWords", encryptionManager.encrypt(words.joinToString(" "))).apply()
    }

    override fun clearAll() {
        App.preferences.edit().clear().apply()
    }

    private val LIGHT_MODE_ENABLED = "light_mode_enabled"
    private val FINGERPRINT_ENABLED = "fingerprint_enabled"

    var isLightModeEnabled: Boolean
        get() = App.preferences.getBoolean(LIGHT_MODE_ENABLED, false)
        set(value) = App.preferences.edit().putBoolean(LIGHT_MODE_ENABLED, value).apply()

    var isFingerprintEnabled: Boolean
        get() = App.preferences.getBoolean(FINGERPRINT_ENABLED, false)
        set(value) = App.preferences.edit().putBoolean(FINGERPRINT_ENABLED, value).apply()

}

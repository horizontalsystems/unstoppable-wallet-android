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

    private val DARK_MODE_ENABLED = "dark_mode_enabled"

    var isDarkModeEnabled: Boolean
        get() = App.preferences.getBoolean(DARK_MODE_ENABLED, true)
        set(value) = App.preferences.edit().putBoolean(DARK_MODE_ENABLED, value).apply()

}

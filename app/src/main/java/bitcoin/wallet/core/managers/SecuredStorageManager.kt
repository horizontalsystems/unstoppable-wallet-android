package bitcoin.wallet.core.managers

import android.text.TextUtils
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IEncryptionManager
import bitcoin.wallet.core.ISecuredStorage


class SecuredStorageManager(private val encryptionManager: IEncryptionManager) : ISecuredStorage {

    private val MNEMONIC_WORDS = "mnemonic_words"
    private val LOCK_PIN = "lock_pin"


    override val savedWords: List<String>?
        get() {
            val string = App.preferences.getString(MNEMONIC_WORDS, null)
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string).split(" ").filter { it.isNotBlank() }
            }
        }

    override fun saveWords(words: List<String>) {
        App.preferences.edit().putString(MNEMONIC_WORDS, encryptionManager.encrypt(words.joinToString(" "))).apply()
    }

    override fun wordsAreEmpty(): Boolean {
        val words = App.preferences.getString(MNEMONIC_WORDS, null)
        return words.isNullOrEmpty()
    }

    override val savedPin: String?
        get() {
            val string = App.preferences.getString(LOCK_PIN, null)
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string)
            }
        }

    override fun savePin(pin: String) {
        App.preferences.edit().putString(LOCK_PIN, encryptionManager.encrypt(pin)).apply()
    }

    override fun pinIsEmpty(): Boolean {
        val string = App.preferences.getString(LOCK_PIN, null)
        return string.isNullOrEmpty()
    }

}

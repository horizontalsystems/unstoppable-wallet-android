package bitcoin.wallet.core.managers

import bitcoin.wallet.core.App
import bitcoin.wallet.core.ILocalStorage

class PreferencesManager : ILocalStorage {

    override val savedWords: List<String>?
        get() {
            val string = App.preferences.getString("mnemonicWords", null)
            return if (string == null) {
                null
            } else {
                string.split(", ").filter { it.isNotBlank() }
            }
        }

    override fun saveWords(words: List<String>) {
        App.preferences.edit().putString("mnemonicWords", words.joinToString(", ")).apply()
    }

}

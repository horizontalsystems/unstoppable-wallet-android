package bitcoin.wallet.core.managers

import bitcoin.wallet.core.App
import bitcoin.wallet.core.ILocalStorage

class PreferencesManager : ILocalStorage {
    override fun saveWords(words: List<String>) {
        App.preferences.edit().putString("mnemonicWords", words.joinToString(", ")).apply()
    }
}

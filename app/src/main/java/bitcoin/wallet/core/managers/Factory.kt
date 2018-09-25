package bitcoin.wallet.core.managers

import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.security.EncryptionManager

object Factory {

    val preferencesManager by lazy {
        PreferencesManager(encryptionManager)
    }

    val randomProvider by lazy {
        RandomProvider()
    }

    val networkManager by lazy {
        NetworkManager()
    }

    val encryptionManager by lazy {
        EncryptionManager()
    }

    val wordsManager: WordsManager by lazy {
        WordsManager(preferencesManager)
    }

}

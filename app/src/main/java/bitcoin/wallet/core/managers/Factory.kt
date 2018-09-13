package bitcoin.wallet.core.managers

import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.security.EncryptionManager

object Factory {

//    val mnemonicManager by lazy {
//        MnemonicManager()
//    }

    val preferencesManager by lazy {
        PreferencesManager(encryptionManager)
    }

    val walletDataProvider by lazy {
        StubWalletDataProvider()
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

}


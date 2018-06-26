package bitcoin.wallet.core.managers

import bitcoin.wallet.core.NetworkManager

object Factory {

    val mnemonicManager by lazy {
        MnemonicManager()
    }

    val preferencesManager by lazy {
        PreferencesManager()
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

}


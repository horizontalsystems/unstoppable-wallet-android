package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.RealmManager

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

    val walletManager by lazy {
        WalletManager()
    }

    val realmManager by lazy {
        RealmManager()
    }

    val loginManager by lazy {
        LoginManager(networkManager, walletManager, realmManager, preferencesManager)
    }

    val databaseManager by lazy {
        DatabaseManager()
    }

    val coinManager by lazy {
        CoinManager()
    }

}


package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.RealmManager
import bitcoin.wallet.core.TransactionConverter
import bitcoin.wallet.core.security.EncryptionManager

object Factory {

    val mnemonicManager by lazy {
        MnemonicManager()
    }

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

    val walletManager by lazy {
        WalletManager()
    }

    val realmManager by lazy {
        RealmManager()
    }

    val databaseManager
        get() = DatabaseManager()

    val coinManager by lazy {
        CoinManager()
    }

    val encryptionManager by lazy {
        EncryptionManager()
    }

    val transactionConverter by lazy {
        TransactionConverter()
    }

}


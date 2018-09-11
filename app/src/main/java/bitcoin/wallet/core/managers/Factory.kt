package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.bitcoin.BitcoinBlockchainService
import bitcoin.wallet.bitcoin.BitcoinJWrapper
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.core.App
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.RealmManager
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
        get() = DatabaseManager(realmManager)

    val coinManager by lazy {
        CoinManager()
    }

    val encryptionManager by lazy {
        EncryptionManager()
    }

    //TODO remove after DI complete
    val blockchainStorage by lazy {
        BlockchainStorage(databaseManager)
    }

    val bitcoinJWrapper by lazy {
        val context = App.instance
        BitcoinJWrapper(context.filesDir, context.resources.assets, true)
    }

    val bitcoinBlockchainService by lazy {
        BitcoinBlockchainService(blockchainStorage, bitcoinJWrapper)
    }

    val blockchainManager by lazy {
        BlockchainManager(bitcoinBlockchainService, preferencesManager)
    }

}


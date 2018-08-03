package bitcoin.wallet.injections.module

import android.content.Context
import bitcoin.wallet.bitcoin.BitcoinBlockchainService
import bitcoin.wallet.bitcoin.BitcoinJWrapper
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.blockchain.IBlockchainService
import bitcoin.wallet.core.IEncryptionManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.managers.PreferencesManager
import bitcoin.wallet.core.security.EncryptionManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MainActivityModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideBitcoinJWrapper(context: Context): BitcoinJWrapper {
        return BitcoinJWrapper(context.filesDir, context.assets, true)
    }

    @Provides
    @Singleton
    fun provideEncryptionManager(): IEncryptionManager {
        return EncryptionManager()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(encryptionManager: IEncryptionManager): ILocalStorage {
        return PreferencesManager(encryptionManager)
    }

    @Provides
    @Singleton
    fun provideBitcoinBlockchainService(blockchainStorage: BlockchainStorage, bitcoinJWrapper: BitcoinJWrapper): IBlockchainService {
        return BitcoinBlockchainService(blockchainStorage, bitcoinJWrapper)
    }

    @Provides
    @Singleton
    fun provideBlockchainManager(bitcoinBlockchainService: IBlockchainService, localStorage: ILocalStorage): BlockchainManager {
        return BlockchainManager(bitcoinBlockchainService, localStorage)
    }

}
package bitcoin.wallet.injections.module

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.core.*
import bitcoin.wallet.core.managers.DatabaseManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {

    @Provides
    @Singleton
    fun provideApp(): App {
        return app
    }

    @Provides
    fun provideRealmManager(): RealmManager {
        return RealmManager()
    }

    @Provides
    fun provideDatabaseManager(realmManager: RealmManager): IDatabaseManager {
        return DatabaseManager(realmManager)
    }

    @Provides
    fun provideBlockchainStorage(databaseManager: IDatabaseManager): BlockchainStorage {
        return BlockchainStorage(databaseManager)
    }

    @Provides
    @Singleton
    fun provideNetworkManager(): INetworkManager {
        return NetworkManager()
    }

    @Provides
    @Singleton
    fun provideExchangeRateService(networkManager: INetworkManager, storage: BlockchainStorage): ExchangeRateService {
        return ExchangeRateService(networkManager, storage)
    }

}
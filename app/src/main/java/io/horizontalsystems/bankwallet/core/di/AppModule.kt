package io.horizontalsystems.bankwallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import javax.inject.Singleton

/**
 * Bridge module: exposes existing App.* singletons to Hilt's graph.
 *
 * During the migration, add a @Provides entry here for each App.* singleton
 * that a newly-annotated @HiltViewModel needs. Once the singleton's own
 * construction is migrated to @Inject constructor, the corresponding
 * @Provides method can be removed and replaced with a @Binds binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWalletManager(): WalletManager = App.walletManager

    @Provides
    @Singleton
    fun provideAccountManager(): IAccountManager = App.accountManager

    @Provides
    @Singleton
    fun provideCoinManager(): ICoinManager = App.coinManager

    @Provides
    @Singleton
    fun provideMarketKit(): MarketKitWrapper = App.marketKit
}

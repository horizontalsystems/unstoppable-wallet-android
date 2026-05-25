package io.horizontalsystems.bankwallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.StellarKitManager
import io.horizontalsystems.bankwallet.core.managers.TonKitManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.managers.TronKitManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import io.horizontalsystems.bankwallet.core.managers.PaidActionSettingsManager
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIconService
import io.horizontalsystems.bankwallet.modules.settings.appearance.LaunchScreenService
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.bankwallet.core.managers.ZanoKitManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.core.ISystemInfoManager
import javax.inject.Singleton

/**
 * Bridge bindings: each @Provides here delegates to an existing App.* singleton.
 * Remove a binding once its target class gains an @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Wallet / Account ---

    @Provides @Singleton
    fun provideWalletManager(): WalletManager = App.walletManager

    @Provides @Singleton
    fun provideBackupManager(): IBackupManager = App.backupManager

    @Provides @Singleton
    fun providePinComponent(): IPinComponent = App.pinComponent

    @Provides @Singleton
    fun provideWCSessionManager(): WCSessionManager = App.wcSessionManager

    @Provides @Singleton
    fun provideWCManager(): WCManager = App.wcManager

    @Provides @Singleton
    fun providePaidActionSettingsManager(): PaidActionSettingsManager = App.paidActionSettingsManager

    @Provides @Singleton
    fun provideAccountManager(): IAccountManager = App.accountManager

    @Provides @Singleton
    fun provideCoinManager(): ICoinManager = App.coinManager

    @Provides @Singleton
    fun provideAdapterManager(): IAdapterManager = App.adapterManager

    @Provides @Singleton
    fun provideTransactionAdapterManager(): TransactionAdapterManager = App.transactionAdapterManager

    @Provides @Singleton
    fun provideSystemInfoManager(): ISystemInfoManager = App.systemInfoManager

    // --- Blockchain kits ---

    @Provides @Singleton
    fun provideEvmBlockchainManager(): EvmBlockchainManager = App.evmBlockchainManager

    @Provides @Singleton
    fun provideBtcBlockchainManager(): BtcBlockchainManager = App.btcBlockchainManager

    @Provides @Singleton
    fun provideTronKitManager(): TronKitManager = App.tronKitManager

    @Provides @Singleton
    fun provideTonKitManager(): TonKitManager = App.tonKitManager

    @Provides @Singleton
    fun provideStellarKitManager(): StellarKitManager = App.stellarKitManager

    @Provides @Singleton
    fun provideSolanaKitManager(): SolanaKitManager = App.solanaKitManager

    @Provides @Singleton
    fun provideZanoKitManager(): ZanoKitManager = App.zanoKitManager

    // --- Market data ---

    @Provides @Singleton
    fun provideMarketKit(): MarketKitWrapper = App.marketKit

    @Provides @Singleton
    fun provideCurrencyManager(): CurrencyManager = App.currencyManager

    @Provides @Singleton
    fun provideNftMetadataManager(): NftMetadataManager = App.nftMetadataManager

    @Provides @Singleton
    fun provideSpamManager(): SpamManager = App.spamManager

    @Provides @Singleton
    fun provideContactsRepository(): ContactsRepository = App.contactsRepository

    // --- Settings helpers ---

    @Provides @Singleton
    fun provideAppConfigProvider(): AppConfigProvider = App.appConfigProvider

    @Provides @Singleton
    fun provideTermsManager(): ITermsManager = App.termsManager

    @Provides @Singleton
    fun provideLanguageManager(): LanguageManager = App.languageManager

    @Provides @Singleton
    fun provideStatsManager(): StatsManager = App.statsManager

    @Provides @Singleton
    fun provideAppIconService(): AppIconService = App.appIconService

    @Provides @Singleton
    fun provideBalanceViewTypeManager(): BalanceViewTypeManager = App.balanceViewTypeManager

    @Provides @Singleton
    fun provideLaunchScreenService(): LaunchScreenService = LaunchScreenService(App.localStorage)

    @Provides @Singleton
    fun provideThemeService(): ThemeService = ThemeService(App.localStorage)

    // --- Storage / display state ---

    @Provides @Singleton
    fun provideLocalStorage(): ILocalStorage = App.localStorage

    @Provides @Singleton
    fun provideBalanceHiddenManager(): BalanceHiddenManager = App.balanceHiddenManager

    @Provides @Singleton
    fun provideEvmLabelManager(): EvmLabelManager = App.evmLabelManager
}

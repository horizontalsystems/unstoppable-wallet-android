package io.horizontalsystems.bankwallet.core.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.AccountCleaner
import io.horizontalsystems.bankwallet.core.managers.AccountManager
import io.horizontalsystems.bankwallet.core.managers.TermsManager
import io.horizontalsystems.bankwallet.core.managers.SystemInfoManager
import io.horizontalsystems.bankwallet.core.managers.CoinManager
import io.horizontalsystems.bankwallet.core.managers.WalletStorage
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.core.managers.BackupManager
import io.horizontalsystems.bankwallet.core.storage.AccountsStorage
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSettingsDao
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.core.managers.NumberFormatter
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.storage.RecentAddressDao
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.core.providers.PredefinedBlockchainSettingsProvider
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import io.horizontalsystems.bankwallet.modules.settings.appearance.LaunchScreenService
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.bankwallet.core.IRestoreSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.RestoreSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EnabledWalletsCacheDao
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ILockoutStorage
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
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
    fun provideBackupManager(impl: BackupManager): IBackupManager = impl

    @Provides @Singleton
    fun providePinComponent(): IPinComponent = App.pinComponent

    @Provides @Singleton
    fun provideRateAppManager(impl: io.horizontalsystems.bankwallet.core.managers.RateAppManager): IRateAppManager = impl

    @Provides @Singleton
    fun provideTorManager(): ITorManager = App.torKitManager

    @Provides @Singleton
    fun provideRootUtil(): RootUtil = RootUtil

    @Provides @Singleton
    fun provideAccountFactory(impl: AccountFactory): IAccountFactory = impl

    @Provides @Singleton
    fun provideAccountManager(impl: AccountManager): IAccountManager = impl

    @Provides @Singleton
    fun provideAccountsStorage(impl: AccountsStorage): IAccountsStorage = impl

    @Provides @Singleton
    fun provideAccountCleaner(impl: AccountCleaner): IAccountCleaner = impl

    @Provides @Singleton
    fun provideCoinManager(impl: CoinManager): ICoinManager = impl

    @Provides @Singleton
    fun provideWalletStorage(impl: WalletStorage): IWalletStorage = impl

    @Provides @Singleton
    fun provideAdapterManager(): IAdapterManager = App.adapterManager

    @Provides @Singleton
    fun provideTransactionAdapterManager(): TransactionAdapterManager = App.transactionAdapterManager

    @Provides @Singleton
    fun provideSystemInfoManager(impl: SystemInfoManager): ISystemInfoManager = impl

    // --- Market data ---

    @Provides @Singleton
    fun provideMarketKit(): MarketKitWrapper = App.marketKit

    @Provides @Singleton
    fun provideNumberFormatter(impl: NumberFormatter): IAppNumberFormatter = impl

    @Provides @Singleton
    fun provideMnemonic(): Mnemonic = Mnemonic()


    @Provides @Singleton
    fun provideThirdKeyboard(impl: io.horizontalsystems.bankwallet.core.managers.LocalStorageManager): IThirdKeyboard = impl

    // --- Settings helpers ---

    @Provides @Singleton
    fun provideTermsManager(impl: TermsManager): ITermsManager = impl

    @Provides @Singleton
    fun provideLaunchScreenService(): LaunchScreenService = LaunchScreenService(App.localStorage)

    @Provides @Singleton
    fun provideThemeService(): ThemeService = ThemeService(App.localStorage)

    // --- Storage / display state ---

    @Provides @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides @Singleton
    fun provideLocalStorage(impl: io.horizontalsystems.bankwallet.core.managers.LocalStorageManager): ILocalStorage = impl

    // --- Main module ---


    @Provides @Singleton
    fun provideKeyStoreManager(): IKeyStoreManager = CoreApp.keyStoreManager

    @Provides @Singleton
    fun provideLockoutStorage(): ILockoutStorage = CoreApp.lockoutStorage

    @Provides @Singleton
    fun provideTonConnectManager(): TonConnectManager = App.tonConnectManager

    @Provides @Singleton
    fun provideNetworkManager(impl: io.horizontalsystems.bankwallet.core.managers.NetworkManager): INetworkManager = impl

    @Provides @Singleton
    fun provideActionCompletedDelegate(): ActionCompletedDelegate = ActionCompletedDelegate

    @Provides @Singleton
    fun provideMarketStorage(impl: io.horizontalsystems.bankwallet.core.managers.LocalStorageManager): IMarketStorage = impl

    @Provides @Singleton
    fun provideMarketFavoritesManager(): MarketFavoritesManager = App.marketFavoritesManager

    @Provides @Singleton
    fun provideMarketWidgetManager(): MarketWidgetManager = App.marketWidgetManager

    // --- Account creation / restore ---


    @Provides @Singleton
    fun provideRestoreSettingsStorage(impl: RestoreSettingsStorage): IRestoreSettingsStorage = impl

    @Provides @Singleton
    fun provideAppDatabase(): AppDatabase = App.appDatabase

    @Provides @Singleton
    fun provideStatsDao(db: AppDatabase) = db.statsDao()

    @Provides @Singleton
    fun provideScannedTransactionDao(db: AppDatabase) = db.scannedTransactionDao()

    @Provides @Singleton
    fun provideEvmAddressLabelDao(db: AppDatabase) = db.evmAddressLabelDao()

    @Provides @Singleton
    fun provideEvmMethodLabelDao(db: AppDatabase) = db.evmMethodLabelDao()

    @Provides @Singleton
    fun provideSyncerStateDao(db: AppDatabase) = db.syncerStateDao()

    @Provides @Singleton
    fun provideTokenAutoEnabledBlockchainDao(db: AppDatabase) = db.tokenAutoEnabledBlockchainDao()

    @Provides @Singleton
    fun provideNftDao(appDatabase: AppDatabase): io.horizontalsystems.bankwallet.core.storage.NftDao = appDatabase.nftDao()

    @Provides @Singleton
    fun provideSwapRecordDao(appDatabase: AppDatabase): io.horizontalsystems.bankwallet.core.storage.SwapRecordDao = appDatabase.swapRecordDao()

    @Provides @Singleton
    fun provideEnabledWalletsCacheDao(appDatabase: AppDatabase): EnabledWalletsCacheDao = appDatabase.enabledWalletsCacheDao()

    @Provides @Singleton
    fun provideRecentAddressDao(appDatabase: AppDatabase): RecentAddressDao = appDatabase.recentAddressDao()

    @Provides @Singleton
    fun provideEnabledWalletStorage(impl: io.horizontalsystems.bankwallet.core.storage.EnabledWalletsStorage): IEnabledWalletStorage = impl

    @Provides @Singleton
    fun provideChartIndicatorSettingsDao(appDatabase: AppDatabase): ChartIndicatorSettingsDao = appDatabase.chartIndicatorSettingsDao()

    @Provides @Singleton
    fun providePredefinedBlockchainSettingsProvider(
        restoreSettingsManager: RestoreSettingsManager,
        zcashBirthdayProvider: ZcashBirthdayProvider,
        moneroBirthdayProvider: MoneroBirthdayProvider,
    ): PredefinedBlockchainSettingsProvider = PredefinedBlockchainSettingsProvider(
        restoreSettingsManager, zcashBirthdayProvider, moneroBirthdayProvider
    )
}

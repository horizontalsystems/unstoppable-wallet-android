package io.horizontalsystems.bankwallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.SwapTermsManager
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupViewItemFactory
import io.horizontalsystems.bankwallet.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.modules.roi.RoiManager
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.bankwallet.core.managers.StellarKitManager
import io.horizontalsystems.bankwallet.core.managers.TonKitManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.managers.TronKitManager
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.PriceManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.core.managers.DonationShowManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.core.providers.PredefinedBlockchainSettingsProvider
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
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
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
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
    fun provideTorManager(): ITorManager = App.torKitManager

    @Provides @Singleton
    fun provideConnectivityManager(): ConnectivityManager = App.connectivityManager

    @Provides @Singleton
    fun provideRateAppManager(): IRateAppManager = App.rateAppManager

    @Provides @Singleton
    fun provideChartIndicatorManager(): ChartIndicatorManager = App.chartIndicatorManager

    @Provides @Singleton
    fun provideRootUtil(): RootUtil = RootUtil

    @Provides @Singleton
    fun provideMoneroNodeManager(): MoneroNodeManager = App.moneroNodeManager

    @Provides @Singleton
    fun provideZanoNodeManager(): ZanoNodeManager = App.zanoNodeManager

    @Provides @Singleton
    fun provideAccountFactory(): IAccountFactory = App.accountFactory

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
    fun provideEvmSyncSourceManager(): EvmSyncSourceManager = App.evmSyncSourceManager

    @Provides @Singleton
    fun provideSolanaRpcSourceManager(): SolanaRpcSourceManager = App.solanaRpcSourceManager

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
    fun provideSwapTermsManager(): SwapTermsManager = App.swapTermsManager

    @Provides @Singleton
    fun provideSwapRecordManager(): SwapRecordManager = App.swapRecordManager

    @Provides @Singleton
    fun provideNumberFormatter(): IAppNumberFormatter = App.numberFormatter

    @Provides @Singleton
    fun provideBackupProvider(): BackupProvider = App.backupProvider

    @Provides @Singleton
    fun provideBackupViewItemFactory(): BackupViewItemFactory = BackupViewItemFactory()

    @Provides @Singleton
    fun provideRoiManager(): RoiManager = App.roiManager

    @Provides @Singleton
    fun provideWordsManager(): WordsManager = App.wordsManager

    @Provides @Singleton
    fun provideThirdKeyboard(): IThirdKeyboard = App.thirdKeyboardStorage

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

    // --- Main module ---

    @Provides @Singleton
    fun provideUserManager(): UserManager = App.userManager

    @Provides @Singleton
    fun provideKeyStoreManager(): IKeyStoreManager = CoreApp.keyStoreManager

    @Provides @Singleton
    fun provideLockoutStorage(): ILockoutStorage = CoreApp.lockoutStorage

    @Provides @Singleton
    fun provideTonConnectManager(): TonConnectManager = App.tonConnectManager

    @Provides @Singleton
    fun provideReleaseNotesManager(): ReleaseNotesManager = App.releaseNotesManager

    @Provides @Singleton
    fun provideDonationShowManager(): DonationShowManager = App.donationShowManager

    @Provides @Singleton
    fun provideNetworkManager(): INetworkManager = App.networkManager

    @Provides @Singleton
    fun provideActionCompletedDelegate(): ActionCompletedDelegate = ActionCompletedDelegate

    @Provides @Singleton
    fun provideBackgroundManager(): BackgroundManager = App.backgroundManager

    @Provides @Singleton
    fun provideMarketStorage(): IMarketStorage = App.marketStorage

    @Provides @Singleton
    fun provideMarketFavoritesManager(): MarketFavoritesManager = App.marketFavoritesManager

    @Provides @Singleton
    fun providePriceManager(): PriceManager = App.priceManager

    @Provides @Singleton
    fun provideMarketWidgetManager(): MarketWidgetManager = App.marketWidgetManager

    // --- Account creation / restore ---

    @Provides @Singleton
    fun provideWalletActivator(): WalletActivator = App.walletActivator

    @Provides @Singleton
    fun providePassphraseValidator(): PassphraseValidator = PassphraseValidator()

    @Provides @Singleton
    fun provideRestoreSettingsManager(): RestoreSettingsManager = App.restoreSettingsManager

    @Provides @Singleton
    fun provideZcashBirthdayProvider(): ZcashBirthdayProvider = App.zcashBirthdayProvider

    @Provides @Singleton
    fun provideMoneroBirthdayProvider(): MoneroBirthdayProvider = App.moneroBirthdayProvider

    @Provides @Singleton
    fun provideEnabledWalletsCacheDao(): EnabledWalletsCacheDao = App.appDatabase.enabledWalletsCacheDao()

    @Provides @Singleton
    fun provideBaseTokenManager(): BaseTokenManager = App.baseTokenManager

    @Provides @Singleton
    fun provideRecentAddressManager(): RecentAddressManager = App.recentAddressManager

    @Provides @Singleton
    fun providePredefinedBlockchainSettingsProvider(
        restoreSettingsManager: RestoreSettingsManager,
        zcashBirthdayProvider: ZcashBirthdayProvider,
        moneroBirthdayProvider: MoneroBirthdayProvider,
    ): PredefinedBlockchainSettingsProvider = PredefinedBlockchainSettingsProvider(
        restoreSettingsManager, zcashBirthdayProvider, moneroBirthdayProvider
    )
}

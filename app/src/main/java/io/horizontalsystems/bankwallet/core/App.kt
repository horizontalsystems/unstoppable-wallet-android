package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.firebase.FirebaseApp
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.factories.AddressParserFactory
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.core.providers.FeeRateProvider
import io.horizontalsystems.bankwallet.core.storage.*
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.launcher.LauncherActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.horizontalsystems.coinkit.CoinKit
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.pin.PinComponent
import io.reactivex.plugins.RxJavaPlugins
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

class App : CoreApp() {

    companion object : ICoreApp by CoreApp {

        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var marketStorage: IMarketStorage
        lateinit var torKitManager: ITorManager
        lateinit var chartTypeStorage: IChartTypeStorage

        lateinit var wordsManager: WordsManager
        lateinit var networkManager: INetworkManager
        lateinit var backgroundStateChangeListener: BackgroundStateChangeListener
        lateinit var appConfigProvider: IAppConfigProvider
        lateinit var adapterManager: IAdapterManager
        lateinit var walletManager: IWalletManager
        lateinit var walletStorage: IWalletStorage
        lateinit var accountManager: IAccountManager
        lateinit var backupManager: IBackupManager
        lateinit var accountCreator: IAccountCreator
        lateinit var predefinedAccountTypeManager: IPredefinedAccountTypeManager

        lateinit var xRateManager: IRateManager
        lateinit var connectivityManager: ConnectivityManager
        lateinit var appDatabase: AppDatabase
        lateinit var accountsStorage: IAccountsStorage
        lateinit var priceAlertManager: IPriceAlertManager
        lateinit var enabledWalletsStorage: IEnabledWalletStorage
        lateinit var blockchainSettingsStorage: IBlockchainSettingsStorage
        lateinit var ethereumKitManager: EthereumKitManager
        lateinit var binanceSmartChainKitManager: BinanceSmartChainKitManager
        lateinit var binanceKitManager: BinanceKitManager
        lateinit var numberFormatter: IAppNumberFormatter
        lateinit var addressParserFactory: AddressParserFactory
        lateinit var feeCoinProvider: FeeCoinProvider
        lateinit var notificationManager: INotificationManager
        lateinit var appVersionManager: AppVersionManager
        lateinit var ethereumRpcModeSettingsManager: IEthereumRpcModeSettingsManager
        lateinit var initialSyncModeSettingsManager: IInitialSyncModeSettingsManager
        lateinit var derivationSettingsManager: IDerivationSettingsManager
        lateinit var bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager
        lateinit var accountCleaner: IAccountCleaner
        lateinit var rateAppManager: IRateAppManager
        lateinit var coinManager: ICoinManager
        lateinit var walletConnectSessionStorage: WalletConnectSessionStorage
        lateinit var walletConnectSessionManager: WalletConnectSessionManager
        lateinit var walletConnectManager: WalletConnectManager
        lateinit var notificationSubscriptionManager: INotificationSubscriptionManager
        lateinit var termsManager: ITermsManager
        lateinit var zcashBirthdayProvider: ZcashBirthdayProvider
        lateinit var marketFavoritesManager: MarketFavoritesManager
        lateinit var coinKit: CoinKit
        lateinit var activateCoinManager: ActivateCoinManager
    }

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) {
            //Disable logging for lower levels in Release build
            Logger.getLogger("").level = Level.SEVERE
        }

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Log.w("RxJava ErrorHandler", e)
        }

        EthereumKit.init()

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val appConfig = AppConfigProvider()
        appConfigProvider = appConfig
        buildConfigProvider = appConfig
        languageConfigProvider = appConfig

        coinKit = CoinKit.create(this, buildConfigProvider.testMode)

        feeRateProvider = FeeRateProvider(appConfigProvider)
        backgroundManager = BackgroundManager(this)

        ethereumKitManager = EthereumKitManager(appConfig.infuraProjectId, appConfig.infuraProjectSecret, appConfig.etherscanApiKey, appConfig.testMode, backgroundManager)
        binanceSmartChainKitManager = BinanceSmartChainKitManager(appConfig.bscscanApiKey, appConfig.testMode, backgroundManager)
        binanceKitManager = BinanceKitManager(buildConfigProvider.testMode)

        appDatabase = AppDatabase.getInstance(this)
        accountsStorage = AccountsStorage(appDatabase)

        AppLog.logsDao = appDatabase.logsDao()

        coinManager = CoinManager(coinKit, appConfigProvider)

        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        blockchainSettingsStorage = BlockchainSettingsStorage(appDatabase)
        walletStorage = WalletStorage(coinManager, enabledWalletsStorage)

        LocalStorageManager(preferences).apply {
            localStorage = this
            chartTypeStorage = this
            pinStorage = this
            thirdKeyboardStorage = this
            themeStorage = this
            marketStorage = this
        }

        torKitManager = TorManager(instance, localStorage)

        wordsManager = WordsManager()
        networkManager = NetworkManager()
        accountCleaner = AccountCleaner(buildConfigProvider.testMode)
        accountManager = AccountManager(accountsStorage, accountCleaner)
        backupManager = BackupManager(accountManager)
        walletManager = WalletManager(accountManager, walletStorage)
        zcashBirthdayProvider = ZcashBirthdayProvider(this)
        accountCreator = AccountCreator(AccountFactory(), wordsManager, zcashBirthdayProvider)
        predefinedAccountTypeManager = PredefinedAccountTypeManager(accountManager, accountCreator)

        KeyStoreManager("MASTER_KEY", KeyStoreCleaner(localStorage, accountManager, walletManager)).apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)

        systemInfoManager = SystemInfoManager()

        languageManager = LanguageManager()
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager(backgroundManager)

        val adapterFactory = AdapterFactory(instance, buildConfigProvider.testMode, ethereumKitManager, binanceSmartChainKitManager, binanceKitManager, backgroundManager)
        adapterManager = AdapterManager(walletManager, adapterFactory, ethereumKitManager, binanceSmartChainKitManager, binanceKitManager)

        initialSyncModeSettingsManager = InitialSyncSettingsManager(coinManager, blockchainSettingsStorage, adapterManager, walletManager)
        derivationSettingsManager = DerivationSettingsManager(blockchainSettingsStorage, adapterManager, walletManager)
        ethereumRpcModeSettingsManager = EthereumRpcModeSettingsManager(blockchainSettingsStorage, adapterManager, walletManager)
        bitcoinCashCoinTypeManager = BitcoinCashCoinTypeManager(walletManager, adapterManager, blockchainSettingsStorage)

        adapterFactory.initialSyncModeSettingsManager = initialSyncModeSettingsManager
        adapterFactory.derivationSettingsManager = derivationSettingsManager
        adapterFactory.ethereumRpcModeSettingsManager = ethereumRpcModeSettingsManager
        adapterFactory.bitcoinCashCoinTypeManager = bitcoinCashCoinTypeManager

        feeCoinProvider = FeeCoinProvider(coinKit)
        xRateManager = RateManager(this, walletManager, currencyManager, feeCoinProvider, appConfigProvider)

        addressParserFactory = AddressParserFactory()

        notificationManager = NotificationManager(NotificationManagerCompat.from(this)).apply {
            backgroundManager.registerListener(this)
        }
        notificationSubscriptionManager = NotificationSubscriptionManager(appDatabase)
        priceAlertManager = PriceAlertManager(appDatabase, notificationSubscriptionManager, xRateManager)

        appVersionManager = AppVersionManager(systemInfoManager, localStorage).apply {
            backgroundManager.registerListener(this)
        }
        pinComponent = PinComponent(
                pinStorage = pinStorage,
                encryptionManager = encryptionManager,
                excludedActivityNames = listOf(
                        KeyStoreActivity::class.java.name,
                        LockScreenActivity::class.java.name,
                        LauncherActivity::class.java.name,
                        TorConnectionActivity::class.java.name
                )
        )

        backgroundStateChangeListener = BackgroundStateChangeListener(systemInfoManager, keyStoreManager, pinComponent).apply {
            backgroundManager.registerListener(this)
        }

        rateAppManager = RateAppManager(walletManager, adapterManager, localStorage)
        walletConnectSessionStorage = WalletConnectSessionStorage(appDatabase)
        walletConnectSessionManager = WalletConnectSessionManager(walletConnectSessionStorage, accountManager)
        walletConnectManager = WalletConnectManager(predefinedAccountTypeManager, ethereumKitManager, binanceSmartChainKitManager)

        termsManager = TermsManager(localStorage)

        marketFavoritesManager = MarketFavoritesManager(appDatabase)

        activateCoinManager = ActivateCoinManager(coinKit, walletManager, accountManager)

        val nightMode = if (CoreApp.themeStorage.isLightModeOn)
            AppCompatDelegate.MODE_NIGHT_NO else
            AppCompatDelegate.MODE_NIGHT_YES

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks(torKitManager))

        startManagers()

        FirebaseApp.initializeApp(this)
    }

    override fun onTrimMemory(level: Int) {
        when (level) {
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                if (backgroundManager.inBackground) {
                    val logger = AppLogger("low memory")
                    logger.info("Kill app due to low memory, level: $level")
                    exitProcess(0)
                }
            }
            else -> {  /*do nothing*/
            }
        }
        super.onTrimMemory(level)
    }

    override fun localizedContext(): Context {
        return localeAwareContext(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAwareContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAwareContext(this)
    }

    private fun startManagers() {
        Thread(Runnable {
            rateAppManager.onAppLaunch()
            accountManager.loadAccounts()
            walletManager.loadWallets()
            adapterManager.preloadAdapters()
            accountManager.clearAccounts()
            notificationSubscriptionManager.processJobs()
        }).start()

        rateAppManager.onAppBecomeActive()
    }
}

package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.factories.*
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.core.storage.*
import io.horizontalsystems.bankwallet.core.utils.EmojiHelper
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoFactory
import io.horizontalsystems.bankwallet.modules.launcher.LauncherActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartActivity
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.pin.PinComponent
import io.horizontalsystems.pin.core.SecureStorage
import io.reactivex.plugins.RxJavaPlugins
import java.util.logging.Level
import java.util.logging.Logger

class App : CoreApp() {

    companion object : ICoreApp by CoreApp {

        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var torKitManager: ITorManager
        lateinit var chartTypeStorage: IChartTypeStorage

        lateinit var wordsManager: WordsManager
        lateinit var networkManager: INetworkManager
        lateinit var backgroundManager: BackgroundManager
        lateinit var keyStoreChangeListener: KeyStoreChangeListener
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
        lateinit var rateStorage: IRateStorage
        lateinit var accountsStorage: IAccountsStorage
        lateinit var priceAlertsStorage: IPriceAlertsStorage
        lateinit var priceAlertManager: PriceAlertManager
        lateinit var enabledWalletsStorage: IEnabledWalletStorage
        lateinit var transactionInfoFactory: FullTransactionInfoFactory
        lateinit var transactionDataProviderManager: TransactionDataProviderManager
        lateinit var ethereumKitManager: IEthereumKitManager
        lateinit var eosKitManager: IEosKitManager
        lateinit var binanceKitManager: BinanceKitManager
        lateinit var numberFormatter: IAppNumberFormatter
        lateinit var addressParserFactory: AddressParserFactory
        lateinit var feeCoinProvider: FeeCoinProvider
        lateinit var priceAlertHandler: IPriceAlertHandler
        lateinit var backgroundPriceAlertManager: IBackgroundPriceAlertManager
        lateinit var emojiHelper: IEmojiHelper
        lateinit var notificationManager: INotificationManager
        lateinit var notificationFactory: INotificationFactory
        lateinit var appStatusManager: IAppStatusManager
        lateinit var appVersionManager: AppVersionManager
        lateinit var backgroundRateAlertScheduler: IBackgroundRateAlertScheduler
        lateinit var blockchainSettingsManager: IBlockchainSettingsManager
        lateinit var accountCleaner: IAccountCleaner
        lateinit var rateCoinMapper: RateCoinMapper
        lateinit var rateAppManager: IRateAppManager
        lateinit var derivationSettingsManager: IDerivationSettingsManager
        lateinit var coinRecordStorage: ICoinRecordStorage
        lateinit var coinManager: ICoinManager
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

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val appConfig = AppConfigProvider()
        appConfigProvider = appConfig
        appConfigTestMode = appConfig
        languageConfigProvider = appConfig

        feeRateProvider = FeeRateProvider(appConfigProvider)
        backgroundManager = BackgroundManager(this)

        ethereumKitManager = EthereumKitManager(appConfig.infuraProjectId, appConfig.infuraProjectSecret, appConfig.etherscanApiKey, appConfig.testMode)
        eosKitManager = EosKitManager(appConfigTestMode.testMode)
        binanceKitManager = BinanceKitManager(appConfigTestMode.testMode)

        appDatabase = AppDatabase.getInstance(this)
        rateStorage = RatesRepository(appDatabase)
        accountsStorage = AccountsStorage(appDatabase)

        coinRecordStorage = CoinRecordStorage(appDatabase)
        coinManager = CoinManager(appConfigProvider, coinRecordStorage)

        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(coinManager, enabledWalletsStorage)

        LocalStorageManager(preferences).apply {
            localStorage = this
            chartTypeStorage = this
            pinStorage = this
            themeStorage = this
        }

        torKitManager = TorManager(instance, localStorage)

        val communicationSettingsManager = CommunicationSettingsManager(appConfigProvider, appDatabase)
        derivationSettingsManager = DerivationSettingsManager(appConfigProvider, appDatabase)
        val syncModeSettingsManager = SyncModeSettingsManager(appConfigProvider, appDatabase)
        blockchainSettingsManager = BlockchainSettingsManager(derivationSettingsManager, syncModeSettingsManager, communicationSettingsManager)

        wordsManager = WordsManager()
        networkManager = NetworkManager()
        accountCleaner = AccountCleaner(appConfigTestMode.testMode)
        accountManager = AccountManager(accountsStorage, accountCleaner)
        backupManager = BackupManager(accountManager)
        walletManager = WalletManager(accountManager, walletStorage)
        accountCreator = AccountCreator(AccountFactory(), wordsManager)
        predefinedAccountTypeManager = PredefinedAccountTypeManager(accountManager, accountCreator)

        KeyStoreManager("MASTER_KEY", KeyStoreCleaner(localStorage, accountManager, walletManager)).apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)
        secureStorage = SecureStorage(encryptionManager)

        systemInfoManager = SystemInfoManager()
        keyStoreChangeListener = KeyStoreChangeListener(systemInfoManager, keyStoreManager).apply {
            backgroundManager.registerListener(this)
        }
        languageManager = LanguageManager()
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager()

        val adapterFactory = AdapterFactory(instance, appConfigTestMode.testMode, ethereumKitManager, eosKitManager, binanceKitManager, blockchainSettingsManager)
        adapterManager = AdapterManager(walletManager, adapterFactory, ethereumKitManager, eosKitManager, binanceKitManager)

        rateCoinMapper = RateCoinMapper()
        xRateManager = RateManager(this, walletManager, currencyManager, rateCoinMapper)

        transactionDataProviderManager = TransactionDataProviderManager(appConfigTestMode.testMode, appConfigProvider.etherscanApiKey, localStorage)
        transactionInfoFactory = FullTransactionInfoFactory(networkManager, transactionDataProviderManager)

        addressParserFactory = AddressParserFactory()
        feeCoinProvider = FeeCoinProvider(appConfigProvider)

        priceAlertsStorage = PriceAlertsStorage(coinManager, appDatabase)
        priceAlertManager = PriceAlertManager(walletManager, priceAlertsStorage)
        emojiHelper = EmojiHelper()
        notificationFactory = NotificationFactory(emojiHelper, instance)
        notificationManager = NotificationManager(NotificationManagerCompat.from(this))
        priceAlertHandler = PriceAlertHandler(priceAlertsStorage, notificationManager, notificationFactory)
        backgroundRateAlertScheduler = BackgroundRateAlertScheduler(instance)
        backgroundPriceAlertManager = BackgroundPriceAlertManager(localStorage, backgroundRateAlertScheduler, priceAlertsStorage, xRateManager, walletStorage, currencyManager, rateStorage, priceAlertHandler, notificationManager).apply {
            backgroundManager.registerListener(this)
        }

        appStatusManager = AppStatusManager(systemInfoManager, localStorage, predefinedAccountTypeManager, walletManager, adapterManager, coinManager, ethereumKitManager, eosKitManager, binanceKitManager)
        appVersionManager = AppVersionManager(systemInfoManager, localStorage).apply {
            backgroundManager.registerListener(this)
        }
        pinComponent = PinComponent(
                application = this,
                securedStorage = secureStorage,
                excludedActivityNames = listOf(
                        LockScreenActivity::class.java.name,
                        LauncherActivity::class.java.name,
                        TorConnectionActivity::class.java.name,
                        RateChartActivity::class.java.name
                ),
                onFire = { activity, requestCode -> LockScreenModule.startForUnlock(activity, requestCode) }
        )

        rateAppManager = RateAppManager(walletManager, adapterManager, localStorage)

        val nightMode = if (CoreApp.themeStorage.isLightModeOn)
            AppCompatDelegate.MODE_NIGHT_NO else
            AppCompatDelegate.MODE_NIGHT_YES

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks(torKitManager))

        startManagers()
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
            priceAlertManager.onAppLaunch()
            backgroundPriceAlertManager.onAppLaunch()
        }).start()

        rateAppManager.onAppBecomeActive()
    }
}

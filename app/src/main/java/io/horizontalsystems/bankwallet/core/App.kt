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
        lateinit var netKitManager: INetManager
        lateinit var chartTypeStorage: IChartTypeStorage

        lateinit var wordsManager: WordsManager
        lateinit var randomManager: IRandomProvider
        lateinit var networkManager: INetworkManager
        lateinit var backgroundManager: BackgroundManager
        lateinit var keyStoreChangeListener: KeyStoreChangeListener
        lateinit var appConfigProvider: IAppConfigProvider
        lateinit var adapterManager: IAdapterManager
        lateinit var walletManager: IWalletManager
        lateinit var walletFactory: IWalletFactory
        lateinit var walletStorage: IWalletStorage
        lateinit var accountManager: IAccountManager
        lateinit var backupManager: IBackupManager
        lateinit var accountCreator: IAccountCreator
        lateinit var predefinedAccountTypeManager: IPredefinedAccountTypeManager
        lateinit var walletRemover: WalletRemover

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
        lateinit var coinSettingsManager: ICoinSettingsManager
        lateinit var accountCleaner: IAccountCleaner
        lateinit var rateCoinMapper: RateCoinMapper
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

        ethereumKitManager = EthereumKitManager(appConfigTestMode)
        eosKitManager = EosKitManager(appConfigTestMode)
        binanceKitManager = BinanceKitManager(appConfigTestMode)

        appDatabase = AppDatabase.getInstance(this)
        rateStorage = RatesRepository(appDatabase)
        accountsStorage = AccountsStorage(appDatabase)

        walletFactory = WalletFactory()
        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(appConfigProvider, walletFactory, enabledWalletsStorage)

        LocalStorageManager().apply {
            localStorage = this
            chartTypeStorage = this
            pinStorage = this
            themeStorage = this
        }

        netKitManager = NetManager(instance, localStorage)

        wordsManager = WordsManager(localStorage)
        networkManager = NetworkManager()
        accountCleaner = AccountCleaner(appConfigTestMode.testMode)
        accountManager = AccountManager(accountsStorage, accountCleaner)
        backupManager = BackupManager(accountManager)
        walletManager = WalletManager(accountManager, walletFactory, walletStorage)
        accountCreator = AccountCreator(AccountFactory(), wordsManager)
        predefinedAccountTypeManager = PredefinedAccountTypeManager(accountManager, accountCreator)
        walletRemover = WalletRemover(accountManager, walletManager)

        KeyStoreManager("MASTER_KEY", KeyStoreCleaner(localStorage, accountManager, walletManager)).apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)
        secureStorage = SecureStorage(encryptionManager)

        randomManager = RandomProvider()
        systemInfoManager = SystemInfoManager()
        keyStoreChangeListener = KeyStoreChangeListener(systemInfoManager, keyStoreManager).apply {
            backgroundManager.registerListener(this)
        }
        languageManager = LanguageManager()
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager()

        adapterManager = AdapterManager(walletManager, AdapterFactory(instance, appConfigTestMode, ethereumKitManager, eosKitManager, binanceKitManager), ethereumKitManager, eosKitManager, binanceKitManager)

        rateCoinMapper = RateCoinMapper()
        xRateManager = RateManager(this, walletManager, currencyManager, rateCoinMapper)

        transactionDataProviderManager = TransactionDataProviderManager(appConfigTestMode, localStorage)
        transactionInfoFactory = FullTransactionInfoFactory(networkManager, transactionDataProviderManager)

        addressParserFactory = AddressParserFactory()
        feeCoinProvider = FeeCoinProvider(appConfigProvider)

        priceAlertsStorage = PriceAlertsStorage(appConfigProvider, appDatabase)
        priceAlertManager = PriceAlertManager(walletManager, priceAlertsStorage)
        emojiHelper = EmojiHelper()
        notificationFactory = NotificationFactory(emojiHelper, instance)
        notificationManager = NotificationManager(NotificationManagerCompat.from(this))
        priceAlertHandler = PriceAlertHandler(priceAlertsStorage, notificationManager, notificationFactory)
        backgroundRateAlertScheduler = BackgroundRateAlertScheduler(instance)
        backgroundPriceAlertManager = BackgroundPriceAlertManager(localStorage, backgroundRateAlertScheduler, priceAlertsStorage, xRateManager, walletStorage, currencyManager, rateStorage, priceAlertHandler, notificationManager).apply {
            backgroundManager.registerListener(this)
        }

        appStatusManager = AppStatusManager(systemInfoManager, localStorage, accountManager, predefinedAccountTypeManager, walletManager, adapterManager, appConfigProvider, ethereumKitManager, eosKitManager, binanceKitManager)
        appVersionManager = AppVersionManager(systemInfoManager, localStorage).apply {
            backgroundManager.registerListener(this)
        }
        coinSettingsManager = CoinSettingsManager(localStorage)
        pinComponent = PinComponent(
                application = this,
                securedStorage = secureStorage,
                excludedActivityNames = listOf(LockScreenActivity::class.java.name, LauncherActivity::class.java.name, TorConnectionActivity::class.java.name),
                onFire = { activity, requestCode  -> LockScreenModule.startForUnlock(activity, requestCode)}
        )

        val nightMode = if (CoreApp.themeStorage.isLightModeOn)
            AppCompatDelegate.MODE_NIGHT_NO else
            AppCompatDelegate.MODE_NIGHT_YES

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks(netKitManager))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAwareContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAwareContext(this)
    }
}

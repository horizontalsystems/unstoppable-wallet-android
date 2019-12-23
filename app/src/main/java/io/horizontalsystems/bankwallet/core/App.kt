package io.horizontalsystems.bankwallet.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.squareup.leakcanary.LeakCanary
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.factories.*
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.core.security.EncryptionManager
import io.horizontalsystems.bankwallet.core.security.KeyStoreManager
import io.horizontalsystems.bankwallet.core.storage.*
import io.horizontalsystems.bankwallet.localehelper.LocaleHelper
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoFactory
import io.horizontalsystems.bankwalval.core.utils.EmojiHelper
import io.reactivex.plugins.RxJavaPlugins
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class App : Application() {

    companion object {

        lateinit var preferences: SharedPreferences

        lateinit var feeRateProvider: FeeRateProvider
        lateinit var secureStorage: ISecuredStorage
        lateinit var localStorage: LocalStorageManager
        lateinit var keyStoreManager: IKeyStoreManager
        lateinit var keyProvider: IKeyProvider
        lateinit var encryptionManager: IEncryptionManager
        lateinit var wordsManager: WordsManager
        lateinit var randomManager: IRandomProvider
        lateinit var networkManager: INetworkManager
        lateinit var currencyManager: ICurrencyManager
        lateinit var backgroundManager: BackgroundManager
        lateinit var languageManager: ILanguageManager
        lateinit var systemInfoManager: ISystemInfoManager
        lateinit var pinManager: IPinManager
        lateinit var lockManager: ILockManager
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

        lateinit var instance: App
            private set

        var lastExitDate: Long = 0
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        if (!BuildConfig.DEBUG) {
            //Disable logging for lower levels in Release build
            Logger.getLogger("").level = Level.SEVERE
        }

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Log.w("RxJava ErrorHandler", e)
        }

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        appConfigProvider = AppConfigProvider()
        feeRateProvider = FeeRateProvider(instance, appConfigProvider)
        backgroundManager = BackgroundManager(this)
        KeyStoreManager("MASTER_KEY").apply {
            keyStoreManager = this
            keyProvider = this
        }
        encryptionManager = EncryptionManager(keyProvider)
        secureStorage = SecuredStorageManager(encryptionManager)
        ethereumKitManager = EthereumKitManager(appConfigProvider)
        eosKitManager = EosKitManager(appConfigProvider)
        binanceKitManager = BinanceKitManager(appConfigProvider)

        appDatabase = AppDatabase.getInstance(this)
        rateStorage = RatesRepository(appDatabase)
        accountsStorage = AccountsStorage(appDatabase)

        walletFactory = WalletFactory()
        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(appConfigProvider, walletFactory, enabledWalletsStorage)
        localStorage = LocalStorageManager()

        wordsManager = WordsManager(localStorage)
        networkManager = NetworkManager()
        accountManager = AccountManager(accountsStorage, AccountCleaner(appConfigProvider.testMode))
        backupManager = BackupManager(accountManager)
        walletManager = WalletManager(accountManager, walletFactory, walletStorage)
        accountCreator = AccountCreator(AccountFactory(), wordsManager)
        predefinedAccountTypeManager = PredefinedAccountTypeManager(accountManager, accountCreator)
        walletRemover = WalletRemover(accountManager, walletManager)

        randomManager = RandomProvider()
        systemInfoManager = SystemInfoManager()
        pinManager = PinManager(secureStorage, localStorage)
        lockManager = LockManager(pinManager).apply {
            backgroundManager.registerListener(this)
        }
        keyStoreChangeListener = KeyStoreChangeListener(systemInfoManager, keyStoreManager).apply {
            backgroundManager.registerListener(this)
        }
        languageManager = LanguageManager(appConfigProvider, "en")
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager()

        adapterManager = AdapterManager(walletManager, AdapterFactory(instance, appConfigProvider, ethereumKitManager, eosKitManager, binanceKitManager), ethereumKitManager, eosKitManager, binanceKitManager)

        xRateManager = RateManager(this, walletManager, currencyManager)

        transactionDataProviderManager = TransactionDataProviderManager(appConfigProvider, localStorage)
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
        coinSettingsManager = CoinSettingsManager()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localeAwareContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeAwareContext(this)
    }

    fun localeAwareContext(base: Context): Context {
        return LocaleHelper.onAttach(base)
    }

    fun getLocale(): Locale {
        return LocaleHelper.getLocale(this)
    }

    fun setLocale(currentLocale: Locale) {
        LocaleHelper.setLocale(this, currentLocale)
    }

    fun isLocaleRTL(): Boolean {
        return LocaleHelper.isRTL(Locale.getDefault())
    }
}

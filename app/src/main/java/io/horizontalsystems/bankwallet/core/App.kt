package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.factories.AddressParserFactory
import io.horizontalsystems.bankwallet.core.factories.EvmAccountManagerFactory
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.EvmLabelProvider
import io.horizontalsystems.bankwallet.core.providers.FeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.FeeTokenProvider
import io.horizontalsystems.bankwallet.core.storage.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.launcher.LauncherActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsRepository
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import io.horizontalsystems.bankwallet.modules.pin.PinComponent
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.storage.ProFeaturesStorage
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WC1SessionStorage
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WC2SessionStorage
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1RequestManager
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetRepository
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.plugins.RxJavaPlugins
import java.util.logging.Level
import java.util.logging.Logger
import androidx.work.Configuration as WorkConfiguration

class App : CoreApp(), WorkConfiguration.Provider, ImageLoaderFactory {

    companion object : ICoreApp by CoreApp {

        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var marketStorage: IMarketStorage
        lateinit var torKitManager: ITorManager
        lateinit var chartTypeStorage: IChartTypeStorage
        lateinit var restoreSettingsStorage: IRestoreSettingsStorage

        lateinit var blockchainSettingsStorage: BlockchainSettingsStorage
        lateinit var btcBlockchainManager: BtcBlockchainManager
        lateinit var wordsManager: WordsManager
        lateinit var networkManager: INetworkManager
        lateinit var backgroundStateChangeListener: BackgroundStateChangeListener
        lateinit var appConfigProvider: AppConfigProvider
        lateinit var adapterManager: IAdapterManager
        lateinit var transactionAdapterManager: TransactionAdapterManager
        lateinit var walletManager: IWalletManager
        lateinit var walletActivator: WalletActivator
        lateinit var walletStorage: IWalletStorage
        lateinit var accountManager: IAccountManager
        lateinit var accountFactory: IAccountFactory
        lateinit var backupManager: IBackupManager
        lateinit var proFeatureAuthorizationManager: ProFeaturesAuthorizationManager
        lateinit var zcashBirthdayProvider: ZcashBirthdayProvider

        lateinit var connectivityManager: ConnectivityManager
        lateinit var appDatabase: AppDatabase
        lateinit var accountsStorage: IAccountsStorage
        lateinit var enabledWalletsStorage: IEnabledWalletStorage
        lateinit var binanceKitManager: BinanceKitManager
        lateinit var numberFormatter: IAppNumberFormatter
        lateinit var addressParserFactory: AddressParserFactory
        lateinit var feeCoinProvider: FeeTokenProvider
        lateinit var accountCleaner: IAccountCleaner
        lateinit var rateAppManager: IRateAppManager
        lateinit var coinManager: ICoinManager
        lateinit var wc1SessionStorage: WC1SessionStorage
        lateinit var wc1SessionManager: WC1SessionManager
        lateinit var wc1RequestManager: WC1RequestManager
        lateinit var wc2Service: WC2Service
        lateinit var wc2SessionManager: WC2SessionManager
        lateinit var wc1Manager: WC1Manager
        lateinit var wc2Manager: WC2Manager
        lateinit var termsManager: ITermsManager
        lateinit var marketFavoritesManager: MarketFavoritesManager
        lateinit var marketKit: MarketKitWrapper
        lateinit var releaseNotesManager: ReleaseNotesManager
        lateinit var restoreSettingsManager: RestoreSettingsManager
        lateinit var evmSyncSourceManager: EvmSyncSourceManager
        lateinit var evmBlockchainManager: EvmBlockchainManager
        lateinit var nftMetadataManager: NftMetadataManager
        lateinit var nftAdapterManager: NftAdapterManager
        lateinit var nftMetadataSyncer: NftMetadataSyncer
        lateinit var evmLabelManager: EvmLabelManager
        lateinit var baseTokenManager: BaseTokenManager
        lateinit var balanceViewTypeManager: BalanceViewTypeManager
        lateinit var balanceHiddenManager: BalanceHiddenManager
        lateinit var marketWidgetManager: MarketWidgetManager
        lateinit var marketWidgetRepository: MarketWidgetRepository
        lateinit var watchAddressBlockchainManager: WatchAddressBlockchainManager
    }

    override val testMode = BuildConfig.testMode

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

        marketKit = MarketKitWrapper(
            context = this,
            hsApiBaseUrl = appConfig.marketApiBaseUrl,
            hsApiKey = appConfig.marketApiKey,
            cryptoCompareApiKey = appConfig.cryptoCompareApiKey,
            defiYieldApiKey = appConfig.defiyieldProviderApiKey
        )
        marketKit.sync()

        feeRateProvider = FeeRateProvider(appConfigProvider)
        backgroundManager = BackgroundManager(this)

        appDatabase = AppDatabase.getInstance(this)

        blockchainSettingsStorage = BlockchainSettingsStorage(appDatabase)
        evmSyncSourceManager = EvmSyncSourceManager(appConfigProvider, blockchainSettingsStorage)

        btcBlockchainManager = BtcBlockchainManager(blockchainSettingsStorage, marketKit)

        binanceKitManager = BinanceKitManager(testMode)

        accountsStorage = AccountsStorage(appDatabase)
        restoreSettingsStorage = RestoreSettingsStorage(appDatabase)

        AppLog.logsDao = appDatabase.logsDao()

        accountCleaner = AccountCleaner(testMode)
        accountManager = AccountManager(accountsStorage, accountCleaner)

        val proFeaturesStorage = ProFeaturesStorage(appDatabase)
        proFeatureAuthorizationManager = ProFeaturesAuthorizationManager(proFeaturesStorage, accountManager, appConfigProvider)

        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(marketKit, enabledWalletsStorage)

        walletManager = WalletManager(accountManager, walletStorage)
        coinManager = CoinManager(marketKit, walletManager)

        blockchainSettingsStorage = BlockchainSettingsStorage(appDatabase)

        LocalStorageManager(preferences).apply {
            localStorage = this
            chartTypeStorage = this
            pinStorage = this
            thirdKeyboardStorage = this
            marketStorage = this
        }

        torKitManager = TorManager(instance, localStorage)

        wordsManager = WordsManager(Mnemonic())
        networkManager = NetworkManager()
        accountFactory = AccountFactory(accountManager)
        backupManager = BackupManager(accountManager)


        KeyStoreManager(
            keyAlias = "MASTER_KEY",
            keyStoreCleaner = KeyStoreCleaner(localStorage, accountManager, walletManager),
            logger = AppLogger("key-store")
        ).apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)

        walletActivator = WalletActivator(walletManager, marketKit)

        val evmAccountManagerFactory = EvmAccountManagerFactory(
            accountManager,
            walletManager,
            marketKit,
            appDatabase.evmAccountStateDao()
        )
        evmBlockchainManager = EvmBlockchainManager(
            backgroundManager,
            evmSyncSourceManager,
            marketKit,
            evmAccountManagerFactory
        )

        systemInfoManager = SystemInfoManager()

        languageManager = LanguageManager()
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager(backgroundManager)

        zcashBirthdayProvider = ZcashBirthdayProvider(this, testMode)
        restoreSettingsManager = RestoreSettingsManager(restoreSettingsStorage, zcashBirthdayProvider)

        evmLabelManager = EvmLabelManager(
            EvmLabelProvider(),
            appDatabase.evmAddressLabelDao(),
            appDatabase.evmMethodLabelDao(),
            appDatabase.syncerStateDao()
        )

        val adapterFactory = AdapterFactory(instance, testMode, btcBlockchainManager, evmBlockchainManager, evmSyncSourceManager, binanceKitManager, backgroundManager, restoreSettingsManager, coinManager, evmLabelManager)
        adapterManager = AdapterManager(walletManager, adapterFactory, btcBlockchainManager, evmBlockchainManager, binanceKitManager)
        transactionAdapterManager = TransactionAdapterManager(adapterManager, adapterFactory)

        feeCoinProvider = FeeTokenProvider(marketKit)

        addressParserFactory = AddressParserFactory()

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

        wc1SessionStorage = WC1SessionStorage(appDatabase)
        wc1SessionManager = WC1SessionManager(wc1SessionStorage, accountManager, evmSyncSourceManager)
        wc1RequestManager = WC1RequestManager()
        wc1Manager = WC1Manager(accountManager, evmBlockchainManager)
        wc2Manager = WC2Manager(accountManager, evmBlockchainManager)

        termsManager = TermsManager(localStorage)

        marketWidgetManager = MarketWidgetManager()
        marketFavoritesManager = MarketFavoritesManager(appDatabase, marketWidgetManager)

        marketWidgetRepository = MarketWidgetRepository(
            marketKit,
            marketFavoritesManager,
            MarketFavoritesMenuService(localStorage, marketWidgetManager),
            TopNftCollectionsRepository(marketKit),
            TopNftCollectionsViewItemFactory(numberFormatter),
            TopPlatformsRepository(marketKit, currencyManager),
            currencyManager
        )

        releaseNotesManager = ReleaseNotesManager(systemInfoManager, localStorage, appConfigProvider)

        setAppTheme()

        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks(torKitManager))



        val nftStorage = NftStorage(appDatabase.nftDao(), marketKit)
        nftMetadataManager = NftMetadataManager(marketKit, appConfigProvider, nftStorage)
        nftAdapterManager = NftAdapterManager(walletManager, evmBlockchainManager)
        nftMetadataSyncer = NftMetadataSyncer(nftAdapterManager, nftMetadataManager, nftStorage)

        initializeWalletConnectV2(appConfig)

        wc2Service = WC2Service()
        wc2SessionManager = WC2SessionManager(accountManager, WC2SessionStorage(appDatabase), wc2Service, wc2Manager)

        baseTokenManager = BaseTokenManager(coinManager, localStorage)
        balanceViewTypeManager = BalanceViewTypeManager(localStorage)
        balanceHiddenManager = BalanceHiddenManager(localStorage)
        watchAddressBlockchainManager = WatchAddressBlockchainManager(
            accountManager,
            walletManager,
            evmBlockchainManager,
            walletActivator
        )

        startTasks()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    private fun initializeWalletConnectV2(appConfig: AppConfigProvider) {
        val initWallet = WalletConnect.Params.Init(
            application = this,
            relayServerUrl = "wss://${appConfig.walletConnectUrl}?projectId=${appConfig.walletConnectProjectId}",
            isController = true,
            metadata = WalletConnect.Model.AppMetaData(
                name = "Unstoppable Wallet",
                description = "Wallet description",
                url = "example.wallet",
                icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media")
            )
        )

//        WalletConnectClient.initialize(initWallet)
    }

    private fun setAppTheme() {
        val nightMode = when (localStorage.currentTheme) {
            ThemeType.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeType.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeType.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    override fun getWorkManagerConfiguration() =
        WorkConfiguration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

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

    private fun startTasks() {
        Thread {
            rateAppManager.onAppLaunch()
            nftMetadataSyncer.start()
            accountManager.loadAccounts()
            walletManager.loadWallets()
            adapterManager.preloadAdapters()
            accountManager.clearAccounts()

            AppVersionManager(systemInfoManager, localStorage).apply { storeAppVersion() }

//            if (!localStorage.favoriteCoinIdsMigrated) {
//                val request = OneTimeWorkRequestBuilder<MigrateFavoriteCoinIdsWorker>().build()
//                WorkManager.getInstance(instance).enqueue(request)
//            }
//            if (!localStorage.fillWalletInfoDone) {
//                val request = OneTimeWorkRequestBuilder<FillWalletInfoWorker>().build()
//                WorkManager.getInstance(instance).enqueue(request)
//            }

            evmLabelManager.sync()

        }.start()
    }
}

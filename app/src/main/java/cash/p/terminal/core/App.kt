package cash.p.terminal.core

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import cash.p.terminal.BuildConfig
import cash.p.terminal.core.di.appModule
import cash.p.terminal.core.factories.AccountFactory
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.notifications.TransactionNotificationCoordinator
import cash.p.terminal.core.notifications.TransactionNotificationManager
import cash.p.terminal.core.managers.AppVersionManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.NftAdapterManager
import cash.p.terminal.core.managers.NftMetadataManager
import cash.p.terminal.core.managers.NftMetadataSyncer
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.managers.StellarAccountManager
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.managers.TonAccountManager
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TronAccountManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.managers.WordsManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.providers.FeeRateProvider
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.storage.BlockchainSettingsStorage
import cash.p.terminal.core.storage.EvmSyncSourceStorage
import cash.p.terminal.core.storage.NftStorage
import cash.p.terminal.modules.backuplocal.fullbackup.BackupProvider
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.market.favorites.MarketFavoritesMenuService
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionsRepository
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import cash.p.terminal.modules.market.topplatforms.TopPlatformsRepository
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.modules.walletconnect.WCDelegate
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import cash.p.terminal.modules.walletconnect.WCWalletRequestHandler
import cash.p.terminal.wallet.IAccountCleaner
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.SubscriptionManager
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.entities.TokenType.AddressSpecType
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.widgets.MarketWidgetManager
import cash.p.terminal.widgets.MarketWidgetRepository
import cash.p.terminal.widgets.MarketWidgetWorker
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.getkeepsafe.relinker.ReLinker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.m2049r.levin.util.NetCipherHelper
import com.m2049r.levin.util.NetCipherHelper.OnStatusChangedListener
import com.m2049r.xmrwallet.model.WalletManager
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLog
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.util.logging.Level
import java.util.logging.Logger
import androidx.work.Configuration as WorkConfiguration

class App : CoreApp(), WorkConfiguration.Provider, SingletonImageLoader.Factory {

    companion object : ICoreApp by CoreApp {
        var sqlCipherLoadFailed = false

        val feeRateProvider: FeeRateProvider by inject(FeeRateProvider::class.java)
        val localStorage: ILocalStorage by inject(ILocalStorage::class.java)
        val marketStorage: IMarketStorage by inject(IMarketStorage::class.java)
        val torKitManager: ITorManager by inject(ITorManager::class.java)
        val currencyManager: CurrencyManager by inject(CurrencyManager::class.java)
        val languageManager: LanguageManager by inject(LanguageManager::class.java)

        val blockchainSettingsStorage: BlockchainSettingsStorage by inject(BlockchainSettingsStorage::class.java)
        val evmSyncSourceStorage: EvmSyncSourceStorage by inject(EvmSyncSourceStorage::class.java)
        val btcBlockchainManager: BtcBlockchainManager by inject(BtcBlockchainManager::class.java)
        val wordsManager: WordsManager by inject(WordsManager::class.java)
        val networkManager: INetworkManager by inject(INetworkManager::class.java)
        val adapterManager: IAdapterManager by inject(AdapterManager::class.java)

        val transactionAdapterManager: TransactionAdapterManager by inject(TransactionAdapterManager::class.java)
        val walletManager: IWalletManager by inject(IWalletManager::class.java)
        val walletActivator: WalletActivator by inject(WalletActivator::class.java)
        val tokenAutoEnableManager: TokenAutoEnableManager by inject(TokenAutoEnableManager::class.java)
        val accountManager: IAccountManager by inject(IAccountManager::class.java)
        val userManager: DefaultUserManager by inject(DefaultUserManager::class.java)
        val accountFactory: IAccountFactory by inject(AccountFactory::class.java)
        val zcashBirthdayProvider: ZcashBirthdayProvider by inject(ZcashBirthdayProvider::class.java)

        val connectivityManager: ConnectivityManager by inject(ConnectivityManager::class.java)
        val appDatabase: AppDatabase by inject(AppDatabase::class.java)
        val enabledWalletsStorage: IEnabledWalletStorage by inject(IEnabledWalletStorage::class.java)
        val tronKitManager: TronKitManager by inject(TronKitManager::class.java)
        val tonKitManager: TonKitManager by inject(TonKitManager::class.java)
        val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
        val rateAppManager: IRateAppManager by inject(IRateAppManager::class.java)
        val coinManager: ICoinManager by inject(ICoinManager::class.java)
        val wcSessionManager: WCSessionManager by inject(WCSessionManager::class.java)
        val wcManager: WCManager by inject(WCManager::class.java)
        lateinit var wcWalletRequestHandler: WCWalletRequestHandler
        val termsManager: ITermsManager by inject(ITermsManager::class.java)
        val marketFavoritesManager: MarketFavoritesManager by inject(MarketFavoritesManager::class.java)
        val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
        val priceManager: PriceManager by inject(PriceManager::class.java)
        val releaseNotesManager: ReleaseNotesManager by inject(ReleaseNotesManager::class.java)
        val evmSyncSourceManager: EvmSyncSourceManager by inject(EvmSyncSourceManager::class.java)
        val evmBlockchainManager: EvmBlockchainManager by inject(EvmBlockchainManager::class.java)
        val solanaRpcSourceManager: SolanaRpcSourceManager by inject(SolanaRpcSourceManager::class.java)
        private val nftStorage: NftStorage by lazy {
            NftStorage(appDatabase.nftDao(), marketKit)
        }
        val nftMetadataManager: NftMetadataManager by lazy {
            NftMetadataManager(marketKit, nftStorage)
        }
        val nftAdapterManager: NftAdapterManager by lazy {
            NftAdapterManager(walletManager, evmBlockchainManager)
        }
        val nftMetadataSyncer: NftMetadataSyncer by lazy {
            NftMetadataSyncer(nftAdapterManager, nftMetadataManager, nftStorage)
        }
        val evmLabelManager: EvmLabelManager by inject(EvmLabelManager::class.java)
        lateinit var baseTokenManager: BaseTokenManager
        lateinit var balanceViewTypeManager: BalanceViewTypeManager
        val balanceHiddenManager: BalanceHiddenManager by inject(IBalanceHiddenManager::class.java)
        val marketWidgetManager: MarketWidgetManager by inject(MarketWidgetManager::class.java)
        val marketWidgetRepository: MarketWidgetRepository by lazy {
            MarketWidgetRepository(
                marketKit = marketKit,
                favoritesManager = marketFavoritesManager,
                favoritesMenuService = MarketFavoritesMenuService(localStorage, marketWidgetManager),
                topNftCollectionsRepository = TopNftCollectionsRepository(marketKit),
                topNftCollectionsViewItemFactory = TopNftCollectionsViewItemFactory(numberFormatter),
                topPlatformsRepository = TopPlatformsRepository(marketKit),
                currencyManager = currencyManager
            )
        }
        val contactsRepository: ContactsRepository by inject(ContactsRepository::class.java)
        val subscriptionManager: SubscriptionManager by inject(SubscriptionManager::class.java)
        val chartIndicatorManager: ChartIndicatorManager by lazy {
            ChartIndicatorManager(appDatabase.chartIndicatorSettingsDao(), localStorage)
        }
        val backupProvider: BackupProvider by lazy {
            BackupProvider(
                localStorage = localStorage,
                languageManager = languageManager,
                walletStorage = enabledWalletsStorage,
                settingsManager = instance.get(),
                accountManager = accountManager,
                accountFactory = accountFactory,
                walletManager = walletManager,
                restoreSettingsManager = instance.get(),
                blockchainSettingsStorage = blockchainSettingsStorage,
                evmBlockchainManager = evmBlockchainManager,
                marketFavoritesManager = marketFavoritesManager,
                balanceViewTypeManager = balanceViewTypeManager,
                appIconService = AppIconService(localStorage),
                themeService = ThemeService(localStorage),
                chartIndicatorManager = chartIndicatorManager,
                chartIndicatorSettingsDao = appDatabase.chartIndicatorSettingsDao(),
                balanceHiddenManager = balanceHiddenManager,
                baseTokenManager = baseTokenManager,
                launchScreenService = LaunchScreenService(localStorage),
                currencyManager = currencyManager,
                btcBlockchainManager = btcBlockchainManager,
                evmSyncSourceManager = evmSyncSourceManager,
                evmSyncSourceStorage = evmSyncSourceStorage,
                solanaRpcSourceManager = solanaRpcSourceManager,
                contactsRepository = contactsRepository
            )
        }
        val tonConnectManager: TonConnectManager by inject(TonConnectManager::class.java)
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val accountCleaner: IAccountCleaner by inject(IAccountCleaner::class.java)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        instance = this

        val errorLoading = preloadSqlCipher()
        if (errorLoading != null) {
            showFatalErrorAndExit(errorLoading)
            return
        }

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        if (!BuildConfig.DEBUG) {
            //Disable logging for lower levels in Release build
            Logger.getLogger("").level = Level.SEVERE
        }

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Log.w("RxJava ErrorHandler", e)
            e?.let {
                if (localStorage.shareCrashDataEnabled) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Timber.tag("RxJava ErrorHandler").e(e ?: return@setErrorHandler)
        }

        pinSettingsStorage = get()
        lockoutStorage = get()
        thirdKeyboardStorage = get()

        backgroundManager = get()

        AppLog.logsDao = appDatabase.logsDao()

        get<KeyStoreManager>().apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)

        systemInfoManager = get()

        pinComponent = get()

        setAppTheme()

        baseTokenManager = BaseTokenManager(coinManager, localStorage)
        balanceViewTypeManager = BalanceViewTypeManager(localStorage)

        get<TransactionNotificationManager>().apply {
            createNotificationChannel()
            createServiceNotificationChannel()
        }

        get<TransactionNotificationCoordinator>().start()

        startTasks()

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled =
            localStorage.shareCrashDataEnabled
    }

    /**
     * Preload sqlcipher library to avoid issues with loading it in some devices
     * (Fatal Exception: java.lang.UnsatisfiedLinkError
     * dlopen failed: library "libsqlcipher.so" not found)
     */
    private fun preloadSqlCipher(): Throwable? {
        val libraryName = "sqlcipher"

        // Attempt 1: System.loadLibrary
        try {
            System.loadLibrary(libraryName)
            Timber.i("sqlcipher library loaded via System.loadLibrary")
            return null
        } catch (error: Throwable) {
            Timber.w(error, "System.loadLibrary failed")
        }

        // Attempt 2: ReLinker (handles corrupted/missing extractions)
        try {
            ReLinker.force().loadLibrary(this, libraryName)
            Timber.i("sqlcipher library loaded via ReLinker")
            return null
        } catch (error: Throwable) {
            Timber.e(error, "All sqlcipher loading methods failed")
            return error
        }
    }

    private fun showFatalErrorAndExit(t: Throwable) {
        Timber.e("FATAL: SQLCipher native library could not be loaded")
        logError(t, "FATAL: SQLCipher native library could not be loaded")

        // Set flag so MainActivity knows to redirect to error screen
        sqlCipherLoadFailed = true
    }

    private fun initCipherForMonero() {
        NetCipherHelper.createInstance(this@App)
        val cipherTag = "MoneroNetCipher"
        NetCipherHelper.register(object : OnStatusChangedListener {
            override fun connected() {
                Timber.tag(cipherTag).d("CONNECTED")
                tryOrNull { WalletManager.getInstance().setProxy(NetCipherHelper.getProxy()) }
            }

            override fun disconnected() {
                Timber.tag(cipherTag).d("DISCONNECTED")
                tryOrNull { WalletManager.getInstance().setProxy("") }
            }

            override fun notInstalled() {
                Timber.tag(cipherTag).d("NOT INSTALLED")
                tryOrNull { WalletManager.getInstance().setProxy("") }
            }

            override fun notEnabled() {
                Timber.tag(cipherTag).d("NOT ENABLED")
                notInstalled()
            }
        })

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (!response.isSuccessful) {
                    // Don't cache error responses (404, 500, etc.)
                    // This prevents placeholder icons from being shown permanently
                    response.newBuilder()
                        .header("Cache-Control", "no-store")
                        .build()
                } else {
                    response
                }
            }
            .build()

        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(okHttpClient))
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    private fun initializeWalletConnectV2() {
        val projectId = AppConfigProvider.walletConnectProjectId
        val serverUrl = "wss://${AppConfigProvider.walletConnectUrl}?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC
        val appMetaData = Core.Model.AppMetaData(
            name = AppConfigProvider.walletConnectAppMetaDataName,
            description = "",
            url = AppConfigProvider.walletConnectAppMetaDataUrl,
            icons = listOf(AppConfigProvider.walletConnectAppMetaDataIcon),
            redirect = null,
        )

        CoreClient.initialize(
            metaData = appMetaData,
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = this,
            onError = { error ->
                logError(error.throwable, "CoreClient.initialize error")
            },
        )
        WalletKit.initialize(Wallet.Params.Init(core = CoreClient)) { error ->
            logError(error.throwable, "WalletKit.initialize error")
        }
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

    override val workManagerConfiguration: WorkConfiguration
        get() = if (BuildConfig.DEBUG) {
            WorkConfiguration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        } else {
            WorkConfiguration.Builder()
                .setMinimumLoggingLevel(Log.ERROR)
                .build()
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

    override val isSwapEnabled = true

    private fun startTasks() {
        coroutineScope.launch {
            initCipherForMonero()

            TronAccountManager(
                accountManager, walletManager, marketKit, tronKitManager,
                tokenAutoEnableManager, get()
            ).start()

            TonAccountManager(
                accountManager, walletManager, tonKitManager,
                tokenAutoEnableManager, get()
            ).start()

            StellarAccountManager(
                accountManager = accountManager,
                walletManager = walletManager,
                stellarKitManager = get(),
                tokenAutoEnableManager = tokenAutoEnableManager,
                userDeletedWalletManager = get()
            ).start()

            wcWalletRequestHandler = WCWalletRequestHandler(evmBlockchainManager)
            initializeWalletConnectV2()
            WCDelegate.initialize()

            EthereumKit.init()
            adapterManager.startAdapterManager()
            marketKit.sync(needForceUpdateCoins())
            rateAppManager.onAppLaunch()
            nftMetadataSyncer.start()
            if (!pinComponent.isPinSet) {
                pinComponent.initDefaultPinLevel()
            }
            clearDeletedAccounts()
            wcSessionManager.start()

            AppVersionManager(systemInfoManager, localStorage).apply { storeAppVersion() }

            if (MarketWidgetWorker.hasEnabledWidgets(instance)) {
                MarketWidgetWorker.enqueueWork(instance)
            } else {
                MarketWidgetWorker.cancel(instance)
            }

            evmLabelManager.sync()
            contactsRepository.initialize()
            AppLog.cleanupOldLogs()
        }
    }

    private fun clearDeletedAccounts() {
        coroutineScope.launch {
            delay(3000)
            accountCleaner.clearAccounts(accountManager.getDeletedAccountIds())
            accountManager.clearDeleted()
        }
    }

    /*** Check if we don't have new tokens in the market kit */
    private fun needForceUpdateCoins(): Boolean {
        val hasZcashShielded = marketKit.token(
            TokenQuery(
                BlockchainType.Zcash, TokenType.AddressSpecTyped(AddressSpecType.Shielded)
            )
        ) != null

        if (!hasZcashShielded) return true

        // Check if Tether has a BSC token (virtual USDT BEP-20)
        val tetherFullCoin = marketKit.fullCoins(listOf("tether")).firstOrNull()
        val hasTetherOnBsc = tetherFullCoin?.tokens?.any {
            it.blockchainType == BlockchainType.BinanceSmartChain
        } == true

        return !hasTetherOnBsc
    }
}

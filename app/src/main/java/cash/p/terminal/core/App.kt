package cash.p.terminal.core

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import cash.p.terminal.BuildConfig
import cash.p.terminal.core.di.appModule
import cash.p.terminal.core.factories.AccountFactory
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.managers.AppVersionManager
import cash.p.terminal.core.managers.BackupManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.CexAssetManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.KeyStoreCleaner
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.LocalStorageManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.NetworkManager
import cash.p.terminal.core.managers.NftAdapterManager
import cash.p.terminal.core.managers.NftMetadataManager
import cash.p.terminal.core.managers.NftMetadataSyncer
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.managers.RateAppManager
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.core.managers.SystemInfoManager
import cash.p.terminal.core.managers.TermsManager
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.managers.TonAccountManager
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TorManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TronAccountManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.core.managers.UserManager
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.managers.WordsManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.providers.CexProviderManager
import cash.p.terminal.core.providers.FeeRateProvider
import cash.p.terminal.core.providers.FeeTokenProvider
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
import cash.p.terminal.modules.pin.PinComponent
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.modules.profeatures.ProFeaturesAuthorizationManager
import cash.p.terminal.modules.profeatures.storage.ProFeaturesStorage
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import cash.p.terminal.modules.walletconnect.WCWalletRequestHandler
import cash.p.terminal.modules.walletconnect.storage.WCSessionStorage
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
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.m2049r.levin.util.NetCipherHelper
import com.m2049r.levin.util.NetCipherHelper.OnStatusChangedListener
import com.m2049r.xmrwallet.model.WalletManager
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLog
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger
import androidx.work.Configuration as WorkConfiguration

class App : CoreApp(), WorkConfiguration.Provider, ImageLoaderFactory {

    companion object : ICoreApp by CoreApp {

        val preferences: SharedPreferences by inject(SharedPreferences::class.java)
        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var marketStorage: IMarketStorage
        lateinit var torKitManager: ITorManager
        val currencyManager: CurrencyManager by inject(CurrencyManager::class.java)
        val languageManager: LanguageManager by inject(LanguageManager::class.java)

        val blockchainSettingsStorage: BlockchainSettingsStorage by inject(BlockchainSettingsStorage::class.java)
        val evmSyncSourceStorage: EvmSyncSourceStorage by inject(EvmSyncSourceStorage::class.java)
        val btcBlockchainManager: BtcBlockchainManager by inject(BtcBlockchainManager::class.java)
        val wordsManager: WordsManager by inject(WordsManager::class.java)
        lateinit var networkManager: INetworkManager
        val appConfigProvider: AppConfigProvider by inject(AppConfigProvider::class.java)
        val adapterManager: IAdapterManager by inject(AdapterManager::class.java)

        val transactionAdapterManager: TransactionAdapterManager by inject(TransactionAdapterManager::class.java)
        val walletManager: IWalletManager by inject(IWalletManager::class.java)
        val walletActivator: WalletActivator by inject(WalletActivator::class.java)
        val tokenAutoEnableManager: TokenAutoEnableManager by inject(TokenAutoEnableManager::class.java)
        val accountManager: IAccountManager by inject(IAccountManager::class.java)
        lateinit var userManager: UserManager
        lateinit var accountFactory: IAccountFactory
        lateinit var backupManager: IBackupManager
        lateinit var proFeatureAuthorizationManager: ProFeaturesAuthorizationManager
        val zcashBirthdayProvider: ZcashBirthdayProvider by inject(ZcashBirthdayProvider::class.java)

        lateinit var connectivityManager: ConnectivityManager
        val appDatabase: AppDatabase by inject(AppDatabase::class.java)
        val enabledWalletsStorage: IEnabledWalletStorage by inject(IEnabledWalletStorage::class.java)
        val solanaKitManager: SolanaKitManager by inject(SolanaKitManager::class.java)
        val tronKitManager: TronKitManager by inject(TronKitManager::class.java)
        val tonKitManager: TonKitManager by inject(TonKitManager::class.java)
        val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
        lateinit var feeCoinProvider: FeeTokenProvider
        lateinit var rateAppManager: IRateAppManager
        val coinManager: ICoinManager by inject(ICoinManager::class.java)
        lateinit var wcSessionManager: WCSessionManager
        lateinit var wcManager: WCManager
        lateinit var wcWalletRequestHandler: WCWalletRequestHandler
        lateinit var termsManager: ITermsManager
        lateinit var marketFavoritesManager: MarketFavoritesManager
        val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
        lateinit var priceManager: PriceManager
        lateinit var releaseNotesManager: ReleaseNotesManager
        val restoreSettingsManager: RestoreSettingsManager by inject(RestoreSettingsManager::class.java)
        val evmSyncSourceManager: EvmSyncSourceManager by inject(EvmSyncSourceManager::class.java)
        val evmBlockchainManager: EvmBlockchainManager by inject(EvmBlockchainManager::class.java)
        val solanaRpcSourceManager: SolanaRpcSourceManager by inject(SolanaRpcSourceManager::class.java)
        lateinit var nftMetadataManager: NftMetadataManager
        lateinit var nftAdapterManager: NftAdapterManager
        lateinit var nftMetadataSyncer: NftMetadataSyncer
        val evmLabelManager: EvmLabelManager by inject(EvmLabelManager::class.java)
        lateinit var baseTokenManager: BaseTokenManager
        lateinit var balanceViewTypeManager: BalanceViewTypeManager
        val balanceHiddenManager: BalanceHiddenManager by inject(IBalanceHiddenManager::class.java)
        lateinit var marketWidgetManager: MarketWidgetManager
        lateinit var marketWidgetRepository: MarketWidgetRepository
        val contactsRepository: ContactsRepository by inject(ContactsRepository::class.java)
        val subscriptionManager: SubscriptionManager by inject(SubscriptionManager::class.java)
        lateinit var cexProviderManager: CexProviderManager
        lateinit var cexAssetManager: CexAssetManager
        lateinit var chartIndicatorManager: ChartIndicatorManager
        lateinit var backupProvider: BackupProvider
        lateinit var spamManager: SpamManager
        lateinit var tonConnectManager: TonConnectManager
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        // For Monero
        initCipherForMonero()

        if (!BuildConfig.DEBUG) {
            //Disable logging for lower levels in Release build
            Logger.getLogger("").level = Level.SEVERE
            // Enable Crashlytics in release builds
        }

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Log.w("RxJava ErrorHandler", e)
            e?.let {
                if (localStorage.shareCrashDataEnabled) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        instance = this

        LocalStorageManager(preferences).apply {
            localStorage = this
            pinSettingsStorage = this
            lockoutStorage = this
            thirdKeyboardStorage = this
            marketStorage = this
        }

        torKitManager = TorManager(instance, localStorage)

        priceManager = PriceManager(localStorage)

        feeRateProvider = FeeRateProvider(appConfigProvider)

        backgroundManager = get()

        AppLog.logsDao = appDatabase.logsDao()

        userManager = UserManager(accountManager)

        val proFeaturesStorage = ProFeaturesStorage(appDatabase)
        proFeatureAuthorizationManager =
            ProFeaturesAuthorizationManager(proFeaturesStorage, accountManager, appConfigProvider)

        networkManager = NetworkManager()
        accountFactory = AccountFactory(accountManager, userManager)
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

        val tronAccountManager = TronAccountManager(
            accountManager,
            walletManager,
            marketKit,
            tronKitManager,
            tokenAutoEnableManager
        )
        tronAccountManager.start()

        val tonAccountManager =
            TonAccountManager(accountManager, walletManager, tonKitManager, tokenAutoEnableManager)
        tonAccountManager.start()

        systemInfoManager = SystemInfoManager(appConfigProvider)
        connectivityManager = ConnectivityManager(backgroundManager)
        val adapterFactory: AdapterFactory = get()

        feeCoinProvider = FeeTokenProvider(marketKit)

        pinComponent = PinComponent(
            pinSettingsStorage = pinSettingsStorage,
            userManager = userManager,
            pinDbStorage = PinDbStorage(appDatabase.pinDao()),
            backgroundManager = backgroundManager
        )

        rateAppManager = RateAppManager(walletManager, adapterManager, localStorage)

        wcManager = WCManager(accountManager)
        wcWalletRequestHandler = WCWalletRequestHandler(evmBlockchainManager)

        termsManager = TermsManager(localStorage)

        marketWidgetManager = MarketWidgetManager()
        marketFavoritesManager =
            MarketFavoritesManager(appDatabase, localStorage, marketWidgetManager)

        marketWidgetRepository = MarketWidgetRepository(
            marketKit = marketKit,
            favoritesManager = marketFavoritesManager,
            favoritesMenuService = MarketFavoritesMenuService(localStorage, marketWidgetManager),
            topNftCollectionsRepository = TopNftCollectionsRepository(marketKit),
            topNftCollectionsViewItemFactory = TopNftCollectionsViewItemFactory(numberFormatter),
            topPlatformsRepository = TopPlatformsRepository(marketKit),
            currencyManager = currencyManager
        )

        releaseNotesManager =
            ReleaseNotesManager(systemInfoManager, localStorage)

        setAppTheme()

        val nftStorage = NftStorage(appDatabase.nftDao(), marketKit)
        nftMetadataManager = NftMetadataManager(marketKit, appConfigProvider, nftStorage)
        nftAdapterManager = NftAdapterManager(walletManager, evmBlockchainManager)
        nftMetadataSyncer = NftMetadataSyncer(nftAdapterManager, nftMetadataManager, nftStorage)

        initializeWalletConnectV2(appConfigProvider)

        wcSessionManager = WCSessionManager(accountManager, WCSessionStorage(appDatabase))

        baseTokenManager = BaseTokenManager(coinManager, localStorage)
        balanceViewTypeManager = BalanceViewTypeManager(localStorage)

        cexProviderManager = CexProviderManager(accountManager)
        cexAssetManager = CexAssetManager(marketKit, appDatabase.cexAssetsDao())
        chartIndicatorManager =
            ChartIndicatorManager(appDatabase.chartIndicatorSettingsDao(), localStorage)

        backupProvider = BackupProvider(
            localStorage = localStorage,
            languageManager = languageManager,
            walletStorage = enabledWalletsStorage,
            settingsManager = restoreSettingsManager,
            accountManager = accountManager,
            accountFactory = accountFactory,
            walletManager = walletManager,
            restoreSettingsManager = restoreSettingsManager,
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

        spamManager = SpamManager(localStorage)

        tonConnectManager = TonConnectManager(
            context = this,
            adapterFactory = adapterFactory,
            appName = "P.cash Wallet",
            appVersion = appConfigProvider.appVersion
        )
        tonConnectManager.start()

        startTasks()

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled =
            localStorage.shareCrashDataEnabled
    }

    private fun initCipherForMonero() {
        NetCipherHelper.createInstance(this@App)
        val cipherTag = "NetCipherHelper"
        NetCipherHelper.register(object : OnStatusChangedListener {
            override fun connected() {
                Timber.tag(cipherTag).d("CONNECTED")
                WalletManager.getInstance().setProxy(NetCipherHelper.getProxy())
            }

            override fun disconnected() {
                Timber.tag(cipherTag).d("DISCONNECTED")
                WalletManager.getInstance().setProxy("")
            }

            override fun notInstalled() {
                Timber.tag(cipherTag).d("NOT INSTALLED")
                WalletManager.getInstance().setProxy("")
            }

            override fun notEnabled() {
                Timber.tag(cipherTag).d("NOT ENABLED")
                notInstalled()
            }
        })

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
        val projectId = appConfig.walletConnectProjectId
        val serverUrl = "wss://${appConfig.walletConnectUrl}?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC
        val appMetaData = Core.Model.AppMetaData(
            name = appConfig.walletConnectAppMetaDataName,
            description = "",
            url = appConfig.walletConnectAppMetaDataUrl,
            icons = listOf(appConfig.walletConnectAppMetaDataIcon),
            redirect = null,
        )

        CoreClient.initialize(
            metaData = appMetaData,
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = this,
            onError = { error ->
                Log.w("AAA", "error", error.throwable)
            },
        )
        Web3Wallet.initialize(Wallet.Params.Init(core = CoreClient)) { error ->
            Log.e("AAA", "error", error.throwable)
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

    override val workManagerConfiguration: androidx.work.Configuration
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

    override fun getApplicationSignatures() = try {
        val signatureList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            ).signingInfo

            when {
                signingInfo?.hasMultipleSigners() == true -> signingInfo.apkContentsSigners // Send all with apkContentsSigners
                else -> signingInfo?.signingCertificateHistory // Send one with signingCertificateHistory
            }
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }

        signatureList?.map {
            val digest = MessageDigest.getInstance("SHA")
            digest.update(it.toByteArray())
            digest.digest()
        } ?: emptyList()
    } catch (e: Exception) {
        // Handle error
        emptyList()
    }

    private fun startTasks() {
        coroutineScope.launch {
            EthereumKit.init()
            adapterManager.startAdapterManager()
            marketKit.sync(needForceUpdateCoins())
            rateAppManager.onAppLaunch()
            nftMetadataSyncer.start()
            pinComponent.initDefaultPinLevel()
            accountManager.clearAccounts()
            wcSessionManager.start()

            AppVersionManager(systemInfoManager, localStorage).apply { storeAppVersion() }

            if (MarketWidgetWorker.hasEnabledWidgets(instance)) {
                MarketWidgetWorker.enqueueWork(instance)
            } else {
                MarketWidgetWorker.cancel(instance)
            }

            evmLabelManager.sync()
            contactsRepository.initialize()
        }
    }

    /*** Check if we don't have new zcash coins in the market kit */
    private fun needForceUpdateCoins() = marketKit.token(
        TokenQuery(
            BlockchainType.Zcash, TokenType.AddressSpecTyped(
                AddressSpecType.Shielded
            )
        )
    ) == null
}

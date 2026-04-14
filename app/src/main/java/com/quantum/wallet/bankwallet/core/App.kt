package com.quantum.wallet.bankwallet.core

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.quantum.wallet.bankwallet.BuildConfig
import com.quantum.wallet.bankwallet.core.factories.AccountFactory
import com.quantum.wallet.bankwallet.core.factories.AdapterFactory
import com.quantum.wallet.bankwallet.core.factories.EvmAccountManagerFactory
import com.quantum.wallet.bankwallet.core.managers.AccountCleaner
import com.quantum.wallet.bankwallet.core.managers.AccountManager
import com.quantum.wallet.bankwallet.core.managers.ActionCompletedDelegate
import com.quantum.wallet.bankwallet.core.managers.AdapterManager
import com.quantum.wallet.bankwallet.core.managers.AppVersionManager
import com.quantum.wallet.bankwallet.core.managers.BackupManager
import com.quantum.wallet.bankwallet.core.managers.BalanceHiddenManager
import com.quantum.wallet.bankwallet.core.managers.BaseTokenManager
import com.quantum.wallet.bankwallet.core.managers.BtcBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.CoinManager
import com.quantum.wallet.bankwallet.core.managers.ConnectivityManager
import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.managers.DonationShowManager
import com.quantum.wallet.bankwallet.core.managers.EvmBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.EvmLabelManager
import com.quantum.wallet.bankwallet.core.managers.EvmSyncSourceManager
import com.quantum.wallet.bankwallet.core.managers.KeyStoreCleaner
import com.quantum.wallet.bankwallet.core.managers.LanguageManager
import com.quantum.wallet.bankwallet.core.managers.LocalStorageManager
import com.quantum.wallet.bankwallet.core.managers.MarketFavoritesManager
import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.core.managers.MigrationManager
import com.quantum.wallet.bankwallet.core.managers.MoneroBirthdayProvider
import com.quantum.wallet.bankwallet.core.managers.MoneroNodeManager
import com.quantum.wallet.bankwallet.core.managers.NetworkManager
import com.quantum.wallet.bankwallet.core.managers.NftAdapterManager
import com.quantum.wallet.bankwallet.core.managers.NftMetadataManager
import com.quantum.wallet.bankwallet.core.managers.NftMetadataSyncer
import com.quantum.wallet.bankwallet.core.managers.NumberFormatter
import com.quantum.wallet.bankwallet.core.managers.PaidActionSettingsManager
import com.quantum.wallet.bankwallet.core.managers.PriceManager
import com.quantum.wallet.bankwallet.core.managers.RateAppManager
import com.quantum.wallet.bankwallet.core.managers.RecentAddressManager
import com.quantum.wallet.bankwallet.core.managers.ReleaseNotesManager
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingsManager
import com.quantum.wallet.bankwallet.core.managers.SolanaKitManager
import com.quantum.wallet.bankwallet.core.managers.SolanaRpcSourceManager
import com.quantum.wallet.bankwallet.core.managers.SolanaWalletManager
import com.quantum.wallet.bankwallet.core.managers.SpamManager
import com.quantum.wallet.bankwallet.core.managers.StellarAccountManager
import com.quantum.wallet.bankwallet.core.managers.StellarKitManager
import com.quantum.wallet.bankwallet.core.managers.SwapTermsManager
import com.quantum.wallet.bankwallet.core.managers.SystemInfoManager
import com.quantum.wallet.bankwallet.core.managers.TermsManager
import com.quantum.wallet.bankwallet.core.managers.TokenAutoEnableManager
import com.quantum.wallet.bankwallet.core.managers.TonAccountManager
import com.quantum.wallet.bankwallet.core.managers.TonConnectManager
import com.quantum.wallet.bankwallet.core.managers.TonKitManager
import com.quantum.wallet.bankwallet.core.managers.TorManager
import com.quantum.wallet.bankwallet.core.managers.TransactionAdapterManager
import com.quantum.wallet.bankwallet.core.managers.TronAccountManager
import com.quantum.wallet.bankwallet.core.managers.TronKitManager
import com.quantum.wallet.bankwallet.core.managers.UserManager
import com.quantum.wallet.bankwallet.core.managers.WalletActivator
import com.quantum.wallet.bankwallet.core.managers.WalletManager
import com.quantum.wallet.bankwallet.core.managers.WalletStorage
import com.quantum.wallet.bankwallet.core.managers.WordsManager
import com.quantum.wallet.bankwallet.core.managers.ZcashBirthdayProvider
import com.quantum.wallet.bankwallet.core.providers.AppConfigProvider
import com.quantum.wallet.bankwallet.core.providers.EvmLabelProvider
import com.quantum.wallet.bankwallet.core.providers.FeeRateProvider
import com.quantum.wallet.bankwallet.core.providers.FeeTokenProvider
import com.quantum.wallet.bankwallet.core.stats.StatsManager
import com.quantum.wallet.bankwallet.core.storage.AccountsStorage
import com.quantum.wallet.bankwallet.core.storage.AppDatabase
import com.quantum.wallet.bankwallet.core.storage.BlockchainSettingsStorage
import com.quantum.wallet.bankwallet.core.storage.EnabledWalletsStorage
import com.quantum.wallet.bankwallet.core.storage.EvmSyncSourceStorage
import com.quantum.wallet.bankwallet.core.storage.MoneroNodeStorage
import com.quantum.wallet.bankwallet.core.storage.NftStorage
import com.quantum.wallet.bankwallet.core.storage.RestoreSettingsStorage
import com.quantum.wallet.bankwallet.core.storage.ScannedTransactionStorage
import com.quantum.wallet.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import com.quantum.wallet.bankwallet.modules.balance.BalanceViewTypeManager
import com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorManager
import com.quantum.wallet.bankwallet.modules.contacts.ContactsRepository
import com.quantum.wallet.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import com.quantum.wallet.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import com.quantum.wallet.bankwallet.modules.multiswap.history.SwapRecordManager
import com.quantum.wallet.bankwallet.modules.multiswap.history.SwapSyncService
import com.quantum.wallet.bankwallet.modules.pin.PinComponent
import com.quantum.wallet.bankwallet.modules.pin.core.PinDbStorage
import com.quantum.wallet.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import com.quantum.wallet.bankwallet.modules.profeatures.storage.ProFeaturesStorage
import com.quantum.wallet.bankwallet.modules.roi.RoiManager
import com.quantum.wallet.bankwallet.modules.settings.appearance.AppIconService
import com.quantum.wallet.bankwallet.modules.settings.appearance.LaunchScreenService
import com.quantum.wallet.bankwallet.modules.theme.ThemeService
import com.quantum.wallet.bankwallet.modules.theme.ThemeType
import com.quantum.wallet.bankwallet.modules.walletconnect.WCManager
import com.quantum.wallet.bankwallet.modules.walletconnect.WCSessionManager
import com.quantum.wallet.bankwallet.modules.walletconnect.WCWalletRequestHandler
import com.quantum.wallet.bankwallet.modules.walletconnect.handler.WCHandlerEvm
import com.quantum.wallet.bankwallet.modules.walletconnect.stellar.WCHandlerStellar
import com.quantum.wallet.bankwallet.modules.walletconnect.storage.WCSessionStorage
import com.quantum.wallet.bankwallet.widgets.MarketWidgetManager
import com.quantum.wallet.bankwallet.widgets.MarketWidgetRepository
import com.quantum.wallet.bankwallet.widgets.MarketWidgetWorker
import com.quantum.wallet.core.CoreApp
import com.quantum.wallet.core.ICoreApp
import com.quantum.wallet.core.security.EncryptionManager
import com.quantum.wallet.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.Mnemonic
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger
import androidx.work.Configuration as WorkConfiguration

class App : CoreApp(), WorkConfiguration.Provider, ImageLoaderFactory {

    companion object : ICoreApp by CoreApp {
        lateinit var backgroundManager: BackgroundManager
        lateinit var preferences: SharedPreferences
        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var marketStorage: IMarketStorage
        lateinit var torKitManager: ITorManager
        lateinit var restoreSettingsStorage: IRestoreSettingsStorage
        lateinit var currencyManager: CurrencyManager
        lateinit var languageManager: LanguageManager

        lateinit var blockchainSettingsStorage: BlockchainSettingsStorage
        lateinit var evmSyncSourceStorage: EvmSyncSourceStorage
        lateinit var btcBlockchainManager: BtcBlockchainManager
        lateinit var wordsManager: WordsManager
        lateinit var networkManager: INetworkManager
        lateinit var appConfigProvider: AppConfigProvider
        lateinit var adapterManager: IAdapterManager
        lateinit var transactionAdapterManager: TransactionAdapterManager
        lateinit var walletManager: WalletManager
        lateinit var walletActivator: WalletActivator
        lateinit var tokenAutoEnableManager: TokenAutoEnableManager
        lateinit var walletStorage: IWalletStorage
        lateinit var accountManager: IAccountManager
        lateinit var userManager: UserManager
        lateinit var accountFactory: IAccountFactory
        lateinit var backupManager: IBackupManager
        lateinit var proFeatureAuthorizationManager: ProFeaturesAuthorizationManager
        lateinit var zcashBirthdayProvider: ZcashBirthdayProvider
        lateinit var moneroBirthdayProvider: MoneroBirthdayProvider

        lateinit var connectivityManager: ConnectivityManager
        lateinit var appDatabase: AppDatabase
        lateinit var accountsStorage: IAccountsStorage
        lateinit var enabledWalletsStorage: IEnabledWalletStorage
        lateinit var solanaKitManager: SolanaKitManager
        lateinit var tronKitManager: TronKitManager
        lateinit var tonKitManager: TonKitManager
        lateinit var stellarKitManager: StellarKitManager
        lateinit var numberFormatter: IAppNumberFormatter
        lateinit var feeCoinProvider: FeeTokenProvider
        lateinit var accountCleaner: IAccountCleaner
        lateinit var rateAppManager: IRateAppManager
        lateinit var coinManager: ICoinManager
        lateinit var wcSessionManager: WCSessionManager
        lateinit var wcManager: WCManager
        lateinit var wcWalletRequestHandler: WCWalletRequestHandler
        lateinit var termsManager: ITermsManager
        lateinit var swapTermsManager: SwapTermsManager
        lateinit var marketFavoritesManager: MarketFavoritesManager
        lateinit var marketKit: MarketKitWrapper
        lateinit var priceManager: PriceManager
        lateinit var releaseNotesManager: ReleaseNotesManager
        lateinit var donationShowManager: DonationShowManager
        lateinit var restoreSettingsManager: RestoreSettingsManager
        lateinit var evmSyncSourceManager: EvmSyncSourceManager
        lateinit var evmBlockchainManager: EvmBlockchainManager
        lateinit var solanaRpcSourceManager: SolanaRpcSourceManager
        lateinit var moneroNodeManager: MoneroNodeManager
        lateinit var moneroNodeStorage: MoneroNodeStorage
        lateinit var nftMetadataManager: NftMetadataManager
        lateinit var nftAdapterManager: NftAdapterManager
        lateinit var nftMetadataSyncer: NftMetadataSyncer
        lateinit var evmLabelManager: EvmLabelManager
        lateinit var baseTokenManager: BaseTokenManager
        lateinit var balanceViewTypeManager: BalanceViewTypeManager
        lateinit var balanceHiddenManager: BalanceHiddenManager
        lateinit var marketWidgetManager: MarketWidgetManager
        lateinit var marketWidgetRepository: MarketWidgetRepository
        lateinit var contactsRepository: ContactsRepository
        lateinit var chartIndicatorManager: ChartIndicatorManager
        lateinit var backupProvider: BackupProvider
        lateinit var scannedTransactionStorage: ScannedTransactionStorage
        lateinit var spamManager: SpamManager
        lateinit var statsManager: StatsManager
        lateinit var tonConnectManager: TonConnectManager
        lateinit var recentAddressManager: RecentAddressManager
        lateinit var roiManager: RoiManager
        lateinit var appIconService: AppIconService
        lateinit var paidActionSettingsManager: PaidActionSettingsManager
        lateinit var swapRecordManager: SwapRecordManager
        lateinit var swapSyncService: SwapSyncService
        var trialExpired: Boolean = false
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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

        LocalStorageManager(preferences).apply {
            localStorage = this
            pinSettingsStorage = this
            lockoutStorage = this
            thirdKeyboardStorage = this
            marketStorage = this
        }

        paidActionSettingsManager = PaidActionSettingsManager(localStorage)

        val appConfig = AppConfigProvider(localStorage)
        appConfigProvider = appConfig

        torKitManager = TorManager(instance, localStorage)

        marketKit = MarketKitWrapper(
            context = this,
            hsApiBaseUrl = appConfig.marketApiBaseUrl,
            hsApiKey = appConfig.marketApiKey,
            newsApiKey = appConfig.newsApiKey,
        )

        priceManager = PriceManager(localStorage)

        feeRateProvider = FeeRateProvider(appConfigProvider)
        backgroundManager = BackgroundManager()

        appDatabase = AppDatabase.getInstance(this)

        blockchainSettingsStorage = BlockchainSettingsStorage(appDatabase)
        evmSyncSourceStorage = EvmSyncSourceStorage(appDatabase)
        evmSyncSourceManager = EvmSyncSourceManager(appConfigProvider, blockchainSettingsStorage, evmSyncSourceStorage)

        btcBlockchainManager = BtcBlockchainManager(blockchainSettingsStorage, marketKit)

        accountsStorage = AccountsStorage(appDatabase)
        restoreSettingsStorage = RestoreSettingsStorage(appDatabase)

        zcashBirthdayProvider = ZcashBirthdayProvider(this)
        moneroBirthdayProvider = MoneroBirthdayProvider()
        restoreSettingsManager = RestoreSettingsManager(restoreSettingsStorage, zcashBirthdayProvider, moneroBirthdayProvider)

        AppLog.logsDao = appDatabase.logsDao()

        accountCleaner = AccountCleaner()
        accountManager = AccountManager(accountsStorage, accountCleaner)
        userManager = UserManager(accountManager)

        val proFeaturesStorage = ProFeaturesStorage(appDatabase)
        proFeatureAuthorizationManager = ProFeaturesAuthorizationManager(proFeaturesStorage, accountManager, appConfigProvider)

        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(marketKit, enabledWalletsStorage)

        walletManager = WalletManager(accountManager, walletStorage)

        moneroNodeStorage = MoneroNodeStorage(appDatabase)
        moneroNodeManager = MoneroNodeManager(blockchainSettingsStorage, moneroNodeStorage, marketKit)
        coinManager = CoinManager(marketKit, walletManager)

        solanaRpcSourceManager = SolanaRpcSourceManager(blockchainSettingsStorage, marketKit)
        val solanaWalletManager = SolanaWalletManager(walletManager, accountManager, marketKit)
        solanaKitManager = SolanaKitManager(appConfigProvider, solanaRpcSourceManager, solanaWalletManager, backgroundManager)

        tronKitManager = TronKitManager(evmSyncSourceManager, backgroundManager)
        tonKitManager = TonKitManager(backgroundManager)
        stellarKitManager = StellarKitManager(backgroundManager)

        wordsManager = WordsManager(Mnemonic())
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

        walletActivator = WalletActivator(walletManager, marketKit)
        tokenAutoEnableManager = TokenAutoEnableManager(appDatabase.tokenAutoEnabledBlockchainDao())

        scannedTransactionStorage = ScannedTransactionStorage(appDatabase.scannedTransactionDao())
        contactsRepository = ContactsRepository(marketKit)
        recentAddressManager = RecentAddressManager(accountManager, appDatabase.recentAddressDao(), ActionCompletedDelegate)
        swapRecordManager = SwapRecordManager(accountManager, appDatabase.swapRecordDao())
        swapSyncService = SwapSyncService(swapRecordManager, appConfigProvider)
        val evmAccountManagerFactory = EvmAccountManagerFactory(
            accountManager,
            walletManager,
            marketKit,
            tokenAutoEnableManager
        )
        evmBlockchainManager = EvmBlockchainManager(
            backgroundManager,
            evmSyncSourceManager,
            marketKit,
            evmAccountManagerFactory
        )

        val tronAccountManager = TronAccountManager(
            accountManager,
            walletManager,
            marketKit,
            tronKitManager,
            tokenAutoEnableManager
        )
        tronAccountManager.start()

        val tonAccountManager = TonAccountManager(accountManager, walletManager, tonKitManager, tokenAutoEnableManager)
        tonAccountManager.start()

        val stellarAccountManager = StellarAccountManager(accountManager, walletManager, stellarKitManager, tokenAutoEnableManager)
        stellarAccountManager.start()

        systemInfoManager = SystemInfoManager(appConfigProvider)

        languageManager = LanguageManager()
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        numberFormatter = NumberFormatter(languageManager)

        connectivityManager = ConnectivityManager(backgroundManager)

        evmLabelManager = EvmLabelManager(
            EvmLabelProvider(),
            appDatabase.evmAddressLabelDao(),
            appDatabase.evmMethodLabelDao(),
            appDatabase.syncerStateDao()
        )

        val adapterFactory = AdapterFactory(
            context = instance,
            btcBlockchainManager = btcBlockchainManager,
            evmBlockchainManager = evmBlockchainManager,
            evmSyncSourceManager = evmSyncSourceManager,
            solanaKitManager = solanaKitManager,
            tronKitManager = tronKitManager,
            tonKitManager = tonKitManager,
            stellarKitManager = stellarKitManager,
            backgroundManager = backgroundManager,
            restoreSettingsManager = restoreSettingsManager,
            coinManager = coinManager,
            evmLabelManager = evmLabelManager,
            localStorage = localStorage,
            moneroNodeManager = moneroNodeManager
        )
        adapterManager = AdapterManager(
            walletManager,
            adapterFactory,
            evmBlockchainManager,
            solanaKitManager,
            tronKitManager,
            tonKitManager,
            stellarKitManager,
        )
        transactionAdapterManager = TransactionAdapterManager(adapterManager, adapterFactory)
        spamManager = SpamManager(localStorage, scannedTransactionStorage, contactsRepository, transactionAdapterManager)

        feeCoinProvider = FeeTokenProvider(marketKit)

        pinComponent = PinComponent(
            context = this,
            pinSettingsStorage = pinSettingsStorage,
            userManager = userManager,
            pinDbStorage = PinDbStorage(appDatabase.pinDao()),
            backgroundManager = backgroundManager,
            localStorage = localStorage
        )

        statsManager = StatsManager(appDatabase.statsDao(), localStorage, marketKit, appConfigProvider, backgroundManager)

        rateAppManager = RateAppManager(walletManager, adapterManager, localStorage)

        wcManager = WCManager(accountManager)
        wcManager.addWcHandler(WCHandlerEvm(evmBlockchainManager))
        wcManager.addWcHandler(WCHandlerStellar(stellarKitManager))
        wcWalletRequestHandler = WCWalletRequestHandler(evmBlockchainManager)

        termsManager = TermsManager(localStorage)
        swapTermsManager = SwapTermsManager(localStorage)

        marketWidgetManager = MarketWidgetManager()
        marketFavoritesManager = MarketFavoritesManager(appDatabase, localStorage, marketWidgetManager)

        marketWidgetRepository = MarketWidgetRepository(
            marketKit,
            marketFavoritesManager,
            MarketFavoritesMenuService(localStorage, marketWidgetManager),
            TopPlatformsRepository(marketKit),
            currencyManager
        )

        releaseNotesManager = ReleaseNotesManager(systemInfoManager, localStorage, appConfigProvider)
        donationShowManager = DonationShowManager(localStorage)

        setAppTheme()

        val nftStorage = NftStorage(appDatabase.nftDao(), marketKit)
        nftMetadataManager = NftMetadataManager(marketKit, appConfigProvider, nftStorage)
        nftAdapterManager = NftAdapterManager(walletManager, evmBlockchainManager)
        nftMetadataSyncer = NftMetadataSyncer(nftAdapterManager, nftMetadataManager, nftStorage)

        initializeWalletConnectV2(appConfig)

        wcSessionManager = WCSessionManager(accountManager, WCSessionStorage(appDatabase))

        baseTokenManager = BaseTokenManager(coinManager, localStorage)
        balanceViewTypeManager = BalanceViewTypeManager(localStorage)
        balanceHiddenManager = BalanceHiddenManager(localStorage, backgroundManager)

        chartIndicatorManager = ChartIndicatorManager(appDatabase.chartIndicatorSettingsDao(), localStorage)

        appIconService = AppIconService(localStorage)

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
            appIconService = appIconService,
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
            moneroNodeManager = moneroNodeManager,
            moneroNodeStorage = moneroNodeStorage,
            contactsRepository = contactsRepository
        )

        tonConnectManager = TonConnectManager(
            context = this,
            adapterFactory = adapterFactory,
            appName = "unstoppable",
            appVersion = appConfigProvider.appVersion
        )
        tonConnectManager.start()

        roiManager = RoiManager(localStorage)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startTasks()
    }

    override fun newImageLoader(): ImageLoader {
        val cacheDir = java.io.File(cacheDir, "http_cache")
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .cache(okhttp3.Cache(cacheDir, 10L * 1024 * 1024)) // 10 MB
            .addNetworkInterceptor(NotFoundCacheInterceptor())
            .build()

        return ImageLoader.Builder(this)
            .crossfade(true)
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(true)
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
                Timber.w(error.throwable)
            },
        )
        WalletKit.initialize(Wallet.Params.Init(core = CoreClient)) { error ->
            Timber.e(error.throwable)
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
            @Suppress("DEPRECATION")
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
            walletManager.start(restoreSettingsManager, moneroNodeManager, btcBlockchainManager, evmBlockchainManager, solanaKitManager, tronKitManager)
            adapterManager.startAdapterManager()
            marketKit.sync()
            rateAppManager.onAppLaunch()
            nftMetadataSyncer.start()
            pinComponent.initDefaultPinLevel()
            accountManager.clearAccounts()
            wcSessionManager.start()
            swapSyncService.start()

            AppVersionManager(systemInfoManager, localStorage).apply { storeAppVersion() }

            if (MarketWidgetWorker.hasEnabledWidgets(instance)) {
                MarketWidgetWorker.enqueueWork(instance)
            } else {
                MarketWidgetWorker.cancel(instance)
            }

            evmLabelManager.sync()
            contactsRepository.initialize()
            trialExpired = !UserSubscriptionManager.hasFreeTrial()
            appIconService.validateAndFixCurrentIcon()
        }

        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> UserSubscriptionManager.onResume()
                    BackgroundManagerState.EnterBackground -> UserSubscriptionManager.pause()
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            delay(3000)
            val migrationManager = MigrationManager(localStorage, termsManager)
            migrationManager.runMigrations()
        }
    }
}

/**
 * OkHttp network interceptor that makes 404 responses cacheable for 24 hours.
 * Without this, 404s have no cache headers and are re-fetched every time.
 * This avoids repeated network requests for missing images (e.g. coin icons),
 * allowing the app to skip straight to the alternative URL on subsequent loads.
 */
private class NotFoundCacheInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val response = chain.proceed(chain.request())
        if (response.code == 404) {
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=86400")
                .removeHeader("Pragma")
                .build()
        }
        return response
    }
}

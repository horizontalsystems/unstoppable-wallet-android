package io.horizontalsystems.bankwallet.core

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
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.factories.EvmAccountManagerFactory
import io.horizontalsystems.bankwallet.core.managers.AccountCleaner
import io.horizontalsystems.bankwallet.core.managers.AccountManager
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.core.managers.AdapterManager
import io.horizontalsystems.bankwallet.core.managers.AppVersionManager
import io.horizontalsystems.bankwallet.core.managers.BackupManager
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.CoinManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.DonationShowManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.KeyStoreCleaner
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.LocalStorageManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.MigrationManager
import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.NetworkManager
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataSyncer
import io.horizontalsystems.bankwallet.core.managers.NumberFormatter
import io.horizontalsystems.bankwallet.core.managers.PriceManager
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.managers.RecentAddressManager
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.core.managers.SolanaWalletManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.StellarAccountManager
import io.horizontalsystems.bankwallet.core.managers.StellarKitManager
import io.horizontalsystems.bankwallet.core.managers.SystemInfoManager
import io.horizontalsystems.bankwallet.core.managers.TermsManager
import io.horizontalsystems.bankwallet.core.managers.TokenAutoEnableManager
import io.horizontalsystems.bankwallet.core.managers.TonAccountManager
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.TonKitManager
import io.horizontalsystems.bankwallet.core.managers.TorManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.managers.TronAccountManager
import io.horizontalsystems.bankwallet.core.managers.TronKitManager
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.managers.WalletStorage
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.EvmLabelProvider
import io.horizontalsystems.bankwallet.core.providers.FeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.FeeTokenProvider
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import io.horizontalsystems.bankwallet.core.storage.AccountsStorage
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EnabledWalletsStorage
import io.horizontalsystems.bankwallet.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.bankwallet.core.storage.MoneroNodeStorage
import io.horizontalsystems.bankwallet.core.storage.NftStorage
import io.horizontalsystems.bankwallet.core.storage.RestoreSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import io.horizontalsystems.bankwallet.modules.pin.PinComponent
import io.horizontalsystems.bankwallet.modules.pin.core.PinDbStorage
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.storage.ProFeaturesStorage
import io.horizontalsystems.bankwallet.modules.roi.RoiManager
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIconService
import io.horizontalsystems.bankwallet.modules.settings.appearance.LaunchScreenService
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCWalletRequestHandler
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.WCHandlerEvm
import io.horizontalsystems.bankwallet.modules.walletconnect.stellar.WCHandlerStellar
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WCSessionStorage
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetRepository
import io.horizontalsystems.bankwallet.widgets.MarketWidgetWorker
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
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
        lateinit var walletManager: IWalletManager
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
        lateinit var spamManager: SpamManager
        lateinit var statsManager: StatsManager
        lateinit var tonConnectManager: TonConnectManager
        lateinit var recentAddressManager: RecentAddressManager
        lateinit var roiManager: RoiManager
        lateinit var appIconService: AppIconService
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

        val appConfig = AppConfigProvider(localStorage)
        appConfigProvider = appConfig

        torKitManager = TorManager(instance, localStorage)

        marketKit = MarketKitWrapper(
            context = this,
            hsApiBaseUrl = appConfig.marketApiBaseUrl,
            hsApiKey = appConfig.marketApiKey,
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

        AppLog.logsDao = appDatabase.logsDao()

        accountCleaner = AccountCleaner()
        accountManager = AccountManager(accountsStorage, accountCleaner)
        userManager = UserManager(accountManager)

        val proFeaturesStorage = ProFeaturesStorage(appDatabase)
        proFeatureAuthorizationManager = ProFeaturesAuthorizationManager(proFeaturesStorage, accountManager, appConfigProvider)

        enabledWalletsStorage = EnabledWalletsStorage(appDatabase)
        walletStorage = WalletStorage(marketKit, enabledWalletsStorage)

        walletManager = WalletManager(accountManager, walletStorage)
        coinManager = CoinManager(marketKit, walletManager)

        solanaRpcSourceManager = SolanaRpcSourceManager(blockchainSettingsStorage, marketKit)
        val solanaWalletManager = SolanaWalletManager(walletManager, accountManager, marketKit)
        solanaKitManager = SolanaKitManager(appConfigProvider, solanaRpcSourceManager, solanaWalletManager, backgroundManager)

        moneroNodeStorage = MoneroNodeStorage(appDatabase)
        moneroNodeManager = MoneroNodeManager(blockchainSettingsStorage, moneroNodeStorage,marketKit)

        tronKitManager = TronKitManager(appConfigProvider, backgroundManager)
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

        spamManager = SpamManager(localStorage, SpamAddressStorage(appDatabase.spamAddressDao()))
        recentAddressManager = RecentAddressManager(accountManager, appDatabase.recentAddressDao(), ActionCompletedDelegate)
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

        zcashBirthdayProvider = ZcashBirthdayProvider(this)
        moneroBirthdayProvider = MoneroBirthdayProvider()
        restoreSettingsManager = RestoreSettingsManager(restoreSettingsStorage, zcashBirthdayProvider, moneroBirthdayProvider)

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
            btcBlockchainManager,
            evmBlockchainManager,
            solanaKitManager,
            tronKitManager,
            tonKitManager,
            stellarKitManager,
            moneroNodeManager
        )
        transactionAdapterManager = TransactionAdapterManager(adapterManager, adapterFactory)

        spamManager.set(transactionAdapterManager)

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

        contactsRepository = ContactsRepository(marketKit)
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

        Timber.plant(Timber.DebugTree())
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
            adapterManager.startAdapterManager()
            marketKit.sync()
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
            val termsManager = TermsManager(localStorage)
            val migrationManager = MigrationManager(localStorage, termsManager)
            migrationManager.runMigrations()
        }
    }
}

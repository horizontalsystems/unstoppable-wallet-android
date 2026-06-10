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
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.KeyStoreCleaner
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.PasskeyManager
import io.horizontalsystems.bankwallet.core.managers.PriceManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.StellarAccountManager
import io.horizontalsystems.bankwallet.core.managers.StellarKitManager
import io.horizontalsystems.bankwallet.core.managers.TokenAutoEnableManager
import io.horizontalsystems.bankwallet.core.managers.TonAccountManager
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.TonKitManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.managers.TronAccountManager
import io.horizontalsystems.bankwallet.core.managers.TronKitManager
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.managers.ZanoKitManager
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.FeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.FeeTokenProvider
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.ZcashEndpointStorage
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.storage.ProFeaturesStorage
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCWalletRequestHandler
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.WCHandlerEvm
import io.horizontalsystems.bankwallet.modules.walletconnect.stellar.WCHandlerStellar
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetRepository
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ICoreApp
import io.horizontalsystems.core.security.EncryptionManager
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.dapp.core.DAppInitParams
import io.horizontalsystems.dapp.core.DAppManager
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger
import androidx.work.Configuration as WorkConfiguration

@HiltAndroidApp
class App : CoreApp(), WorkConfiguration.Provider, ImageLoaderFactory {

    companion object : ICoreApp by CoreApp {
        lateinit var backgroundManager: BackgroundManager
        lateinit var preferences: SharedPreferences
        lateinit var feeRateProvider: FeeRateProvider
        lateinit var localStorage: ILocalStorage
        lateinit var marketStorage: IMarketStorage
        lateinit var torKitManager: ITorManager
        lateinit var currencyManager: CurrencyManager
        lateinit var languageManager: LanguageManager

        lateinit var blockchainSettingsStorage: BlockchainSettingsStorage
        lateinit var btcBlockchainManager: BtcBlockchainManager
        lateinit var appConfigProvider: AppConfigProvider
        lateinit var adapterManager: IAdapterManager
        lateinit var transactionAdapterManager: TransactionAdapterManager
        lateinit var walletManager: WalletManager
        lateinit var passkeyManager: PasskeyManager
        lateinit var tokenAutoEnableManager: TokenAutoEnableManager
        lateinit var accountManager: IAccountManager
        lateinit var userManager: UserManager
        lateinit var accountFactory: IAccountFactory
        lateinit var proFeatureAuthorizationManager: ProFeaturesAuthorizationManager
        lateinit var zcashBirthdayProvider: ZcashBirthdayProvider
        lateinit var moneroBirthdayProvider: MoneroBirthdayProvider

        lateinit var connectivityManager: ConnectivityManager
        lateinit var appDatabase: AppDatabase
        lateinit var solanaKitManager: SolanaKitManager
        lateinit var tronKitManager: TronKitManager
        lateinit var tonKitManager: TonKitManager
        lateinit var stellarKitManager: StellarKitManager
        lateinit var numberFormatter: IAppNumberFormatter
        lateinit var feeCoinProvider: FeeTokenProvider
        lateinit var rateAppManager: IRateAppManager
        lateinit var coinManager: ICoinManager
        lateinit var wcSessionManager: WCSessionManager
        lateinit var wcManager: WCManager
        lateinit var wcWalletRequestHandler: WCWalletRequestHandler
        lateinit var termsManager: ITermsManager
        lateinit var marketFavoritesManager: MarketFavoritesManager
        lateinit var marketKit: MarketKitWrapper
        lateinit var priceManager: PriceManager
        lateinit var restoreSettingsManager: RestoreSettingsManager
        lateinit var evmSyncSourceManager: EvmSyncSourceManager
        lateinit var evmBlockchainManager: EvmBlockchainManager
        lateinit var solanaRpcSourceManager: SolanaRpcSourceManager
        lateinit var moneroNodeManager: MoneroNodeManager
        lateinit var zanoNodeManager: ZanoNodeManager
        lateinit var zanoKitManager: ZanoKitManager
        lateinit var zcashEndpointStorage: ZcashEndpointStorage
        lateinit var zcashEndpointManager: ZcashLightWalletEndpointManager
        lateinit var evmLabelManager: EvmLabelManager
        lateinit var marketWidgetManager: MarketWidgetManager
        lateinit var marketWidgetRepository: MarketWidgetRepository
        lateinit var contactsRepository: ContactsRepository
        lateinit var chartIndicatorManager: ChartIndicatorManager
        lateinit var spamManager: SpamManager
        lateinit var statsManager: StatsManager
        lateinit var tonConnectManager: TonConnectManager
        var trialExpired: Boolean = false
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

        val localStorageManager = EntryPointAccessors
            .fromApplication(this, StorageEntryPoint::class.java)
            .localStorageManager()
        localStorage = localStorageManager
        pinSettingsStorage = localStorageManager
        lockoutStorage = localStorageManager
        thirdKeyboardStorage = localStorageManager
        marketStorage = localStorageManager

        val appConfig = EntryPointAccessors
            .fromApplication(this, AppConfigEntryPoint::class.java)
            .appConfigProvider()
        appConfigProvider = appConfig

        torKitManager = EntryPointAccessors
            .fromApplication(this, TorEntryPoint::class.java)
            .torManager()

        marketKit = EntryPointAccessors
            .fromApplication(this, MarketKitEntryPoint::class.java)
            .marketKit()

        feeRateProvider = FeeRateProvider(appConfigProvider)
        backgroundManager = EntryPointAccessors
            .fromApplication(this, BackgroundManagerEntryPoint::class.java)
            .backgroundManager()

        val configLeavesEntryPoint = EntryPointAccessors
            .fromApplication(this, ConfigLeavesEntryPoint::class.java)
        priceManager = configLeavesEntryPoint.priceManager()
        termsManager = configLeavesEntryPoint.termsManager()
        connectivityManager = configLeavesEntryPoint.connectivityManager()

        appDatabase = AppDatabase.getInstance(this)

        val blockchainManagersEntryPoint = EntryPointAccessors
            .fromApplication(this, BlockchainManagersEntryPoint::class.java)
        blockchainSettingsStorage = blockchainManagersEntryPoint.blockchainSettingsStorage()
        evmSyncSourceManager = EntryPointAccessors
            .fromApplication(this, EvmSyncSourceEntryPoint::class.java)
            .evmSyncSourceManager()

        btcBlockchainManager = blockchainManagersEntryPoint.btcBlockchainManager()

        val restoreSettingsEntryPoint = EntryPointAccessors
            .fromApplication(this, RestoreSettingsEntryPoint::class.java)
        zcashBirthdayProvider = restoreSettingsEntryPoint.zcashBirthdayProvider()
        moneroBirthdayProvider = restoreSettingsEntryPoint.moneroBirthdayProvider()
        restoreSettingsManager = restoreSettingsEntryPoint.restoreSettingsManager()

        AppLog.logsDao = appDatabase.logsDao()

        accountManager = EntryPointAccessors
            .fromApplication(this, AccountCoreEntryPoint::class.java)
            .accountManager()
        val accountWrappersEntryPoint = EntryPointAccessors
            .fromApplication(this, AccountWrappersEntryPoint::class.java)
        userManager = accountWrappersEntryPoint.userManager()

        val proFeaturesStorage = ProFeaturesStorage(appDatabase)
        proFeatureAuthorizationManager = ProFeaturesAuthorizationManager(proFeaturesStorage, accountManager, appConfigProvider)


        val walletCoreEntryPoint = EntryPointAccessors
            .fromApplication(this, WalletCoreEntryPoint::class.java)
        walletManager = walletCoreEntryPoint.walletManager()

        val nodeSettingsEntryPoint = EntryPointAccessors
            .fromApplication(this, NodeSettingsEntryPoint::class.java)
        moneroNodeManager = nodeSettingsEntryPoint.moneroNodeManager()
        zanoNodeManager = nodeSettingsEntryPoint.zanoNodeManager()
        val kitManagersEntryPoint = EntryPointAccessors
            .fromApplication(this, KitManagersEntryPoint::class.java)
        zanoKitManager = kitManagersEntryPoint.zanoKitManager()
        zcashEndpointStorage = kitManagersEntryPoint.zcashEndpointStorage()
        zcashEndpointManager = kitManagersEntryPoint.zcashEndpointManager()
        coinManager = walletCoreEntryPoint.coinManager()

        solanaRpcSourceManager = kitManagersEntryPoint.solanaRpcSourceManager()
        solanaKitManager = kitManagersEntryPoint.solanaKitManager()

        tronKitManager = kitManagersEntryPoint.tronKitManager()
        tonKitManager = kitManagersEntryPoint.tonKitManager()
        stellarKitManager = kitManagersEntryPoint.stellarKitManager()

        accountFactory = accountWrappersEntryPoint.accountFactory()


        KeyStoreManager(
            keyAlias = "MASTER_KEY",
            keyStoreCleaner = KeyStoreCleaner(localStorage, accountManager, walletManager),
            logger = AppLogger("key-store")
        ).apply {
            keyStoreManager = this
            keyProvider = this
        }

        encryptionManager = EncryptionManager(keyProvider)

        passkeyManager = PasskeyManager()
        val evmLabelEntryPoint = EntryPointAccessors
            .fromApplication(this, EvmLabelEntryPoint::class.java)
        tokenAutoEnableManager = evmLabelEntryPoint.tokenAutoEnableManager()
        evmLabelManager = evmLabelEntryPoint.evmLabelManager()

        val miscDataEntryPoint = EntryPointAccessors
            .fromApplication(this, MiscDataEntryPoint::class.java)
        contactsRepository = miscDataEntryPoint.contactsRepository()
        chartIndicatorManager = miscDataEntryPoint.chartIndicatorManager()
        evmBlockchainManager = EntryPointAccessors
            .fromApplication(this, EvmBlockchainEntryPoint::class.java)
            .evmBlockchainManager()

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


        val localizationEntryPoint = EntryPointAccessors
            .fromApplication(this, LocalizationEntryPoint::class.java)
        languageManager = localizationEntryPoint.languageManager()
        currencyManager = localizationEntryPoint.currencyManager()
        numberFormatter = localizationEntryPoint.numberFormatter()


        val adapterEntryPoint = EntryPointAccessors
            .fromApplication(this, AdapterEntryPoint::class.java)
        val adapterFactory = adapterEntryPoint.adapterFactory()
        adapterManager = adapterEntryPoint.adapterManager()
        transactionAdapterManager = adapterEntryPoint.transactionAdapterManager()

        feeCoinProvider = FeeTokenProvider(marketKit)

        pinComponent = EntryPointAccessors
            .fromApplication(this, PinEntryPoint::class.java)
            .pinComponent()

        val spamStatsEntryPoint = EntryPointAccessors
            .fromApplication(this, SpamStatsEntryPoint::class.java)
        spamManager = spamStatsEntryPoint.spamManager()
        statsManager = spamStatsEntryPoint.statsManager()

        val wcRateEntryPoint = EntryPointAccessors
            .fromApplication(this, WcRateEntryPoint::class.java)
        rateAppManager = wcRateEntryPoint.rateAppManager()
        wcSessionManager = wcRateEntryPoint.wcSessionManager()

        wcManager = wcRateEntryPoint.wcManager()
        wcManager.addWcHandler(WCHandlerEvm(evmBlockchainManager))
        wcManager.addWcHandler(WCHandlerStellar(stellarKitManager))
        wcWalletRequestHandler = WCWalletRequestHandler(evmBlockchainManager)


        marketWidgetManager = MarketWidgetManager()
        marketFavoritesManager = MarketFavoritesManager(appDatabase, localStorage, marketWidgetManager)

        marketWidgetRepository = MarketWidgetRepository(
            marketKit,
            marketFavoritesManager,
            MarketFavoritesMenuService(localStorage, marketWidgetManager),
            TopPlatformsRepository(marketKit),
            currencyManager
        )

        setAppTheme()


        DAppManager.initialize(
            params = DAppInitParams(
                application = this,
                projectId = appConfig.walletConnectProjectId,
                relayServerUrl = "wss://${appConfig.walletConnectUrl}?projectId=${appConfig.walletConnectProjectId}",
                appName = appConfig.walletConnectAppMetaDataName,
                appUrl = appConfig.walletConnectAppMetaDataUrl,
                appIcon = appConfig.walletConnectAppMetaDataIcon,
            ),
            callback = WCDelegate,
        )




        tonConnectManager = EntryPointAccessors
            .fromApplication(this, TonConnectEntryPoint::class.java)
            .tonConnectManager()
        tonConnectManager.start()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        EntryPointAccessors.fromApplication(this, AppInitializerEntryPoint::class.java)
            .appInitializer()
            .start()
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

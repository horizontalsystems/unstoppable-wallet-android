package io.horizontalsystems.bankwallet.core

import android.content.Context
import io.horizontalsystems.bankwallet.core.managers.AppVersionManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.MigrationManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataSyncer
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
import io.horizontalsystems.bankwallet.core.managers.TronKitManager
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.multiswap.history.SwapSyncService
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIconService
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.widgets.MarketWidgetWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the eager startup sequence that must run at app launch, extracted from App.onCreate().
 *
 * The order of operations is significant: it is preserved exactly as it was in App.startTasks().
 * Keep this an explicit, reviewable manifest of launch-time work — when the dependencies move to
 * Hilt, only how this class is constructed should change, not what start() does.
 */
@Singleton
class AppInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletManager: WalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val moneroNodeManager: MoneroNodeManager,
    private val zanoNodeManager: ZanoNodeManager,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val adapterManager: IAdapterManager,
    private val marketKit: MarketKitWrapper,
    private val rateAppManager: IRateAppManager,
    private val nftMetadataSyncer: NftMetadataSyncer,
    private val pinComponent: IPinComponent,
    private val accountManager: IAccountManager,
    private val wcSessionManager: WCSessionManager,
    private val swapSyncService: SwapSyncService,
    private val systemInfoManager: ISystemInfoManager,
    private val localStorage: ILocalStorage,
    private val evmLabelManager: EvmLabelManager,
    private val contactsRepository: ContactsRepository,
    private val appIconService: AppIconService,
    private val backgroundManager: BackgroundManager,
    private val termsManager: ITermsManager,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        coroutineScope.launch {
            EthereumKit.init()
            walletManager.start(restoreSettingsManager, moneroNodeManager, zanoNodeManager, btcBlockchainManager, evmBlockchainManager, solanaKitManager, tronKitManager)
            adapterManager.startAdapterManager()
            marketKit.sync()
            rateAppManager.onAppLaunch()
            nftMetadataSyncer.start()
            pinComponent.initDefaultPinLevel()
            accountManager.clearAccounts()
            wcSessionManager.start()
            swapSyncService.start()

            AppVersionManager(systemInfoManager, localStorage).apply { storeAppVersion() }

            if (MarketWidgetWorker.hasEnabledWidgets(context)) {
                MarketWidgetWorker.enqueueWork(context)
            } else {
                MarketWidgetWorker.cancel(context)
            }

            evmLabelManager.sync()
            contactsRepository.initialize()
            App.trialExpired = !UserSubscriptionManager.hasFreeTrial()
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppInitializerEntryPoint {
    fun appInitializer(): AppInitializer
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageEntryPoint {
    fun localStorageManager(): io.horizontalsystems.bankwallet.core.managers.LocalStorageManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RestoreSettingsEntryPoint {
    fun restoreSettingsManager(): io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
    fun zcashBirthdayProvider(): io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
    fun moneroBirthdayProvider(): io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NodeSettingsEntryPoint {
    fun moneroNodeManager(): io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
    fun zanoNodeManager(): io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EvmSyncSourceEntryPoint {
    fun evmSyncSourceManager(): io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccountCoreEntryPoint {
    fun accountManager(): IAccountManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccountWrappersEntryPoint {
    fun userManager(): io.horizontalsystems.bankwallet.core.managers.UserManager
    fun accountFactory(): io.horizontalsystems.bankwallet.core.IAccountFactory
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WalletCoreEntryPoint {
    fun walletManager(): io.horizontalsystems.bankwallet.core.managers.WalletManager
    fun coinManager(): io.horizontalsystems.bankwallet.core.ICoinManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfigLeavesEntryPoint {
    fun priceManager(): io.horizontalsystems.bankwallet.core.managers.PriceManager
    fun termsManager(): io.horizontalsystems.bankwallet.core.ITermsManager
    fun connectivityManager(): io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MiscDataEntryPoint {
    fun chartIndicatorManager(): io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
    fun contactsRepository(): io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EvmLabelEntryPoint {
    fun evmLabelManager(): io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
    fun tokenAutoEnableManager(): io.horizontalsystems.bankwallet.core.managers.TokenAutoEnableManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SpamStatsEntryPoint {
    fun spamManager(): io.horizontalsystems.bankwallet.core.managers.SpamManager
    fun statsManager(): io.horizontalsystems.bankwallet.core.stats.StatsManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocalizationEntryPoint {
    fun languageManager(): io.horizontalsystems.bankwallet.core.managers.LanguageManager
    fun currencyManager(): io.horizontalsystems.bankwallet.core.managers.CurrencyManager
    fun numberFormatter(): io.horizontalsystems.bankwallet.core.IAppNumberFormatter
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KitManagersEntryPoint {
    fun zanoKitManager(): io.horizontalsystems.bankwallet.core.managers.ZanoKitManager
    fun solanaRpcSourceManager(): io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
    fun solanaKitManager(): io.horizontalsystems.bankwallet.core.managers.SolanaKitManager
    fun tronKitManager(): io.horizontalsystems.bankwallet.core.managers.TronKitManager
    fun tonKitManager(): io.horizontalsystems.bankwallet.core.managers.TonKitManager
    fun stellarKitManager(): io.horizontalsystems.bankwallet.core.managers.StellarKitManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockchainManagersEntryPoint {
    fun blockchainSettingsStorage(): io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
    fun btcBlockchainManager(): io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WcRateEntryPoint {
    fun rateAppManager(): io.horizontalsystems.bankwallet.core.IRateAppManager
    fun wcManager(): io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
    fun wcSessionManager(): io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EvmBlockchainEntryPoint {
    fun evmBlockchainManager(): io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
}

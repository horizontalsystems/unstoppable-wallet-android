package cash.p.terminal.di

import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.preference.PreferenceManager
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IBackupManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IMarketStorage
import cash.p.terminal.core.IRateAppManager
import cash.p.terminal.core.ITermsManager
import cash.p.terminal.core.ITorManager
import cash.p.terminal.core.address.AddressCheckManager
import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.deeplink.DeeplinkParser
import cash.p.terminal.core.factories.AccountFactory
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.managers.AppHeadersProviderImpl
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.core.managers.BackupManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import android.content.Context
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.notifications.NotificationDeduplicator
import cash.p.terminal.core.notifications.TransactionMonitor
import cash.p.terminal.core.notifications.TransactionNotificationCoordinator
import cash.p.terminal.core.notifications.TransactionNotificationManager
import cash.p.terminal.core.notifications.polling.BtcLikeTransactionsPoller
import cash.p.terminal.core.notifications.polling.EvmTransactionsPoller
import cash.p.terminal.core.notifications.polling.MoneroTransactionsPoller
import cash.p.terminal.core.notifications.polling.SolanaTransactionsPoller
import cash.p.terminal.core.notifications.polling.StellarTransactionsPoller
import cash.p.terminal.core.notifications.polling.TonTransactionsPoller
import cash.p.terminal.core.notifications.polling.TransactionPollingManager
import cash.p.terminal.core.notifications.polling.TronTransactionsPoller
import cash.p.terminal.core.notifications.polling.ZcashTransactionsPoller
import cash.p.terminal.core.managers.CreateRequiredTokensUseCaseImpl
import cash.p.terminal.core.managers.DefaultCurrencyManager
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.GetTonAddressUseCaseImpl
import cash.p.terminal.core.managers.KeyStoreCleaner
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.LocalStorageManager
import cash.p.terminal.core.managers.MoneroKitManager
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.managers.PendingBalanceCalculator
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.core.managers.PendingTransactionRegistrarImpl
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.managers.RateAppManager
import cash.p.terminal.core.managers.RecentAddressManager
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SeedPhraseQrCrypto
import cash.p.terminal.core.managers.SilentCameraManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.managers.SolanaWalletManager
import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.core.managers.StellarKitManager
import cash.p.terminal.core.managers.SystemInfoManager
import cash.p.terminal.core.managers.TermsManager
import cash.p.terminal.core.managers.TimePasswordProvider
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TorManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.managers.WordsManager
import cash.p.terminal.core.INetworkManager
import cash.p.terminal.core.managers.NetworkManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.providers.CompositeFallbackAddressProvider
import cash.p.terminal.core.providers.PendingAccountProvider
import cash.p.terminal.core.providers.PendingAccountProviderImpl
import cash.p.terminal.core.providers.PredefinedBlockchainSettingsProvider
import cash.p.terminal.core.providers.TonFallbackAddressProvider
import cash.p.terminal.wallet.FallbackAddressProvider
import cash.p.terminal.feature.miniapp.domain.storage.IUniqueCodeStorage
import cash.p.terminal.feature.miniapp.domain.usecase.CreateRequiredTokensUseCase
import cash.p.terminal.feature.miniapp.domain.usecase.GetTonAddressUseCase
import cash.p.terminal.manager.IConnectivityManager
import cash.p.terminal.modules.addtoken.AddTokenService
import cash.p.terminal.modules.market.favorites.MarketFavoritesMenuService
import cash.p.terminal.modules.market.favorites.MarketFavoritesRepository
import cash.p.terminal.modules.market.favorites.MarketFavoritesService
import cash.p.terminal.modules.pin.PinComponent
import cash.p.terminal.modules.pin.core.ILockoutManager
import cash.p.terminal.modules.pin.core.ILockoutUntilDateFactory
import cash.p.terminal.modules.pin.core.LockoutManager
import cash.p.terminal.modules.pin.core.LockoutUntilDateFactory
import cash.p.terminal.modules.pin.core.OneTimeTimer
import cash.p.terminal.modules.pin.core.UptimeProvider
import cash.p.terminal.modules.pin.hiddenwallet.HiddenWalletPinPolicy
import cash.p.terminal.modules.transactions.CheckAmlIncomingTransactionUseCase
import cash.p.terminal.modules.transactions.TransactionSyncStateRepository
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.widgets.MarketWidgetManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import cash.p.terminal.modules.walletconnect.handler.WCHandlerEvm
import cash.p.terminal.modules.walletconnect.stellar.WCHandlerStellar
import cash.p.terminal.modules.walletconnect.storage.WCSessionStorage
import cash.p.terminal.network.alphaaml.api.AlphaAmlApi
import cash.p.terminal.network.data.AppHeadersProvider
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.managers.ITransactionHiddenManager
import cash.p.terminal.wallet.managers.UserManager
import com.m2049r.xmrwallet.service.MoneroWalletService
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.CurrentDateProvider
import io.horizontalsystems.core.ICurrentDateProvider
import io.horizontalsystems.core.IKeyProvider
import io.horizontalsystems.core.IKeyStoreCleaner
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ILanguageManager
import io.horizontalsystems.core.ILockoutStorage
import io.horizontalsystems.core.ILoggingSettings
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinSettingsStorage
import io.horizontalsystems.core.ISilentPhotoCapture
import io.horizontalsystems.core.ISmsNotificationSettings
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.core.security.KeyStoreManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val managerModule = module {
    singleOf(::NetworkManager) bind INetworkManager::class
    singleOf(::SystemInfoManager) bind ISystemInfoManager::class
    singleOf(::BackupManager) bind IBackupManager::class
    singleOf(::LanguageManager) bind ILanguageManager::class
    singleOf(::AppHeadersProviderImpl) bind AppHeadersProvider::class
    singleOf(::DefaultCurrencyManager) bind CurrencyManager::class
    singleOf(::SolanaRpcSourceManager)
    singleOf(::TonFallbackAddressProvider)
    single<FallbackAddressProvider> {
        CompositeFallbackAddressProvider(
            providers = listOf(get<TonFallbackAddressProvider>())
        )
    }
    singleOf(::AdapterManager) bind IAdapterManager::class
    singleOf(::LocalStorageManager) {
        bind<ILocalStorage>()
        bind<ILoggingSettings>()
        bind<ISmsNotificationSettings>()
        bind<IPinSettingsStorage>()
        bind<ILockoutStorage>()
        bind<IThirdKeyboard>()
        bind<IMarketStorage>()
        bind<IUniqueCodeStorage>()
    }
    single { PreferenceManager.getDefaultSharedPreferences(get()) }
    singleOf(::BackgroundManager)
    singleOf(::BackgroundKeepAliveManager)
    singleOf(::TransactionNotificationManager)
    single {
        val prefs = get<Context>()
            .getSharedPreferences("notification_dedup", Context.MODE_PRIVATE)
        NotificationDeduplicator(prefs)
    }
    singleOf(::EvmTransactionsPoller)
    singleOf(::TonTransactionsPoller)
    singleOf(::TronTransactionsPoller)
    singleOf(::SolanaTransactionsPoller)
    singleOf(::StellarTransactionsPoller)
    singleOf(::BtcLikeTransactionsPoller)
    singleOf(::ZcashTransactionsPoller)
    singleOf(::MoneroTransactionsPoller)
    single {
        TransactionPollingManager(
            listOf(
                get<EvmTransactionsPoller>(),
                get<TonTransactionsPoller>(),
                get<TronTransactionsPoller>(),
                get<SolanaTransactionsPoller>(),
                get<StellarTransactionsPoller>(),
                get<BtcLikeTransactionsPoller>(),
                get<ZcashTransactionsPoller>(),
                get<MoneroTransactionsPoller>(),
            ),
            get()
        )
    }
    singleOf(::TransactionMonitor)
    singleOf(::TransactionNotificationCoordinator)
    singleOf(::ConnectivityManager) bind IConnectivityManager::class
    singleOf(::EvmSyncSourceManager)
    singleOf(::TokenAutoEnableManager)
    singleOf(::EvmBlockchainManager)
    singleOf(::BtcBlockchainManager)
    singleOf(::SolanaKitManager)
    singleOf(::StellarKitManager)
    singleOf(::TonKitManager)
    singleOf(::GetTonAddressUseCaseImpl) bind GetTonAddressUseCase::class
    singleOf(::CreateRequiredTokensUseCaseImpl) bind CreateRequiredTokensUseCase::class
    singleOf(::TronKitManager)
    factoryOf(::StackingManager)
    singleOf(::RestoreSettingsManager)
    singleOf(::TimePasswordProvider)
    singleOf(::SeedPhraseQrCrypto)
    singleOf(::EvmLabelManager)
    factoryOf(::SolanaWalletManager)
    singleOf(::RecentAddressManager)
    singleOf(::AmlStatusManager)
    singleOf(::DefaultUserManager) bind UserManager::class
    single<IPinComponent> {
        PinComponent(
            pinSettingsStorage = get(),
            userManager = get(),
            pinDbStorage = get(),
            backgroundManager = get(),
            resetUseCase = get(),
            deleteAllContactsUseCase = get(),
            dispatcherProvider = get()
        )
    }
    singleOf(::AccountFactory) bind IAccountFactory::class
    singleOf(::RateAppManager) bind IRateAppManager::class
    singleOf(::TermsManager) bind ITermsManager::class
    singleOf(::ReleaseNotesManager)
    singleOf(::WCSessionStorage)
    single {
        WCManager(accountManager = get()).also {
            it.addWcHandler(WCHandlerEvm(get()))
            it.addWcHandler(WCHandlerStellar(get()))
        }
    }
    singleOf(::WCSessionManager)
    factoryOf(::WalletActivator)
    factoryOf(::AddTokenService)

    singleOf(::Mnemonic)
    factoryOf(::WordsManager)

    singleOf(::MoneroKitManager)
    singleOf(::MoneroWalletService)
    singleOf(::SilentCameraManager) bind ISilentPhotoCapture::class
    singleOf(::CurrentDateProvider) bind ICurrentDateProvider::class
    singleOf(::UptimeProvider)
    singleOf(::LockoutUntilDateFactory) bind ILockoutUntilDateFactory::class
    singleOf(::LockoutManager) bind ILockoutManager::class
    factoryOf(::OneTimeTimer)
    singleOf(::GlanceAppWidgetManager)

    singleOf(::SpamManager)
    singleOf(::PoisonAddressManager)
    singleOf(::AddressCheckManager)
    singleOf(::DeeplinkParser)
    singleOf(::CheckAmlIncomingTransactionUseCase)
    singleOf(::TransactionAdapterManager)
    singleOf(::TransactionSyncStateRepository)
    singleOf(::BalanceHiddenManager) bind IBalanceHiddenManager::class
    singleOf(::TransactionHiddenManager) bind ITransactionHiddenManager::class
    singleOf(::TorManager) bind ITorManager::class
    singleOf(::PredefinedBlockchainSettingsProvider)
    singleOf(::KeyStoreCleaner) bind IKeyStoreCleaner::class
    single<KeyStoreManager.Logger> { AppLogger("key-store") }
    single {
        KeyStoreManager(
            keyAlias = "MASTER_KEY",
            keyStoreCleaner = get(),
            logger = get()
        )
    }
    single<IKeyStoreManager> { get<KeyStoreManager>() }
    single<IKeyProvider> { get<KeyStoreManager>() }
    single {
        TonConnectManager(
            context = get(),
            adapterFactory = get(),
            appName = "P.cash Wallet",
            appVersion = AppConfigProvider.appVersion
        ).also { it.start() }
    }
    factory { (pinComponent: IPinComponent) ->
        HiddenWalletPinPolicy(pinComponent, get())
    }

    // Network APIs
    single {
        AlphaAmlApi(
            httpClient = get(),
            baseUrl = AppConfigProvider.alphaAmlBaseUrl,
            apiKey = AppConfigProvider.alphaAmlApiKey
        )
    }

    // Market favorites
    singleOf(::MarketWidgetManager)
    singleOf(::PriceManager)
    singleOf(::MarketFavoritesManager)
    singleOf(::MarketFavoritesRepository)
    singleOf(::MarketFavoritesMenuService)
    factoryOf(::MarketFavoritesService)

    // Pending transactions
    singleOf(::PendingTransactionRepository)
    singleOf(::PendingBalanceCalculator)
    singleOf(::PendingTransactionRegistrarImpl) bind PendingTransactionRegistrar::class
    singleOf(::PendingTransactionMatcher)
    singleOf(::PendingAccountProviderImpl) bind PendingAccountProvider::class
    singleOf(::PendingTransactionConverter)
}

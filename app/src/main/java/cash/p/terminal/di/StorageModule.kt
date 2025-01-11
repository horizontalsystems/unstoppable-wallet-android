package cash.p.terminal.di

import androidx.preference.PreferenceManager
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IRestoreSettingsStorage
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.core.factories.EvmAccountManagerFactory
import cash.p.terminal.core.managers.AccountCleaner
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.managers.BinanceKitManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.CoinManager
import cash.p.terminal.core.managers.DefaultCurrencyManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.LocalStorageManager
import cash.p.terminal.core.managers.NumberFormatter
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.managers.SolanaWalletManager
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.providers.EvmLabelProvider
import cash.p.terminal.core.storage.AccountsStorage
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.storage.BlockchainSettingsStorage
import cash.p.terminal.core.storage.EnabledWalletsStorage
import cash.p.terminal.core.storage.EvmSyncSourceStorage
import cash.p.terminal.core.storage.RestoreSettingsStorage
import cash.p.terminal.modules.balance.DefaultBalanceService
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.IAccountCleaner
import cash.p.terminal.wallet.IAccountsStorage
import cash.p.terminal.wallet.IAdapterManager
import io.horizontalsystems.core.IAppNumberFormatter
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.balance.BalanceXRateRepository
import io.horizontalsystems.core.BackgroundManager
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val storageModule = module {
    single { AppDatabase.getInstance(get()) }
    singleOf(::LanguageManager)
    singleOf(::NumberFormatter) bind IAppNumberFormatter::class
    singleOf(::DefaultCurrencyManager) bind CurrencyManager::class
    singleOf(::BinanceKitManager)
    singleOf(::SolanaRpcSourceManager)
    singleOf(::RestoreSettingsStorage) bind IRestoreSettingsStorage::class
    singleOf(::CoinManager) bind ICoinManager::class
    singleOf(::AccountsStorage) bind IAccountsStorage::class
    singleOf(::AdapterManager) bind IAdapterManager::class
    singleOf(::AccountCleaner) bind IAccountCleaner::class
    singleOf(::LocalStorageManager) bind ILocalStorage::class
    singleOf(::EnabledWalletsStorage) bind IEnabledWalletStorage::class
    single { PreferenceManager.getDefaultSharedPreferences(get()) }
    singleOf(::AppConfigProvider)
    singleOf(::BlockchainSettingsStorage)
    singleOf(::BackgroundManager)
    singleOf(::EvmSyncSourceManager)
    singleOf(::TokenAutoEnableManager)
    singleOf(::EvmSyncSourceStorage)
    singleOf(::EvmBlockchainManager)
    singleOf(::BtcBlockchainManager)
    singleOf(::SolanaKitManager)
    singleOf(::TonKitManager)
    singleOf(::TronKitManager)
    singleOf(::RestoreSettingsManager)
    singleOf(::ZcashBirthdayProvider)
    singleOf(::EvmLabelManager)
    singleOf(::EvmLabelProvider)

    factoryOf(::EvmAccountManagerFactory)
    singleOf(::AdapterFactory)
    factoryOf(::SolanaWalletManager)
    factory<BalanceXRateRepository>(named("wallet")) {
        DefaultBalanceXRateRepository(
            tag = "wallet",
            currencyManager = get(),
            marketKit = get()
        )
    }
    factory<BalanceService>(named("wallet")) {
        DefaultBalanceService.getInstance("wallet")
    }
    /*factory<BalanceXRateRepository>(named("wallet")) {
        DefaultBalanceXRateRepository(
            tag = "wallet",
            currencyManager = get(),
            marketKit = get()
        )
    }*/
    factory { get<AppDatabase>().evmAddressLabelDao() }
    factory { get<AppDatabase>().evmMethodLabelDao() }
    factory { get<AppDatabase>().syncerStateDao() }
    factory { get<AppDatabase>().tokenAutoEnabledBlockchainDao() }
}
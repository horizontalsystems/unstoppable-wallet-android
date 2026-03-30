package cash.p.terminal.di

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.IRestoreSettingsStorage
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.core.factories.EvmAccountManagerFactory
import cash.p.terminal.core.managers.AccountCleaner
import cash.p.terminal.core.managers.CoinManager
import cash.p.terminal.core.managers.DeletedWalletChecker
import cash.p.terminal.core.managers.NumberFormatter
import cash.p.terminal.core.managers.UserDeletedWalletManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.wallet.IDeletedWalletChecker
import cash.p.terminal.core.providers.EvmLabelProvider
import cash.p.terminal.core.providers.FeeRateProvider
import cash.p.terminal.core.storage.AccountsStorage
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.storage.BlockchainSettingsStorage
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.storage.EnabledWalletsStorage
import cash.p.terminal.core.storage.EvmSyncSourceStorage
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.storage.PendingTransactionStorage
import cash.p.terminal.core.storage.RestoreSettingsStorage
import cash.p.terminal.core.storage.SpamAddressStorage
import cash.p.terminal.core.storage.ZcashSingleUseAddressStorage
import cash.p.terminal.core.adapters.zcash.ZcashSingleUseAddressManager
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.modules.balance.DefaultBalanceService
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.send.address.AddressCheckerControl
import cash.p.terminal.modules.send.address.AddressCheckerControlImpl
import cash.p.terminal.modules.transactions.ITransactionRecordRepository
import cash.p.terminal.modules.transactions.TransactionRecordRepository
import cash.p.terminal.modules.transactions.TransactionViewItemFactory
import cash.p.terminal.core.utils.SwapTransactionMatcher
import cash.p.terminal.wallet.IAccountCleaner
import cash.p.terminal.wallet.IAccountsStorage
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.balance.BalanceXRateRepository
import io.horizontalsystems.core.IAppNumberFormatter
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val storageModule = module {
    single { AppDatabase.getInstance(get()) }
    singleOf(::NumberFormatter) bind IAppNumberFormatter::class
    singleOf(::RestoreSettingsStorage) bind IRestoreSettingsStorage::class
    singleOf(::CoinManager) bind ICoinManager::class
    singleOf(::AccountsStorage) bind IAccountsStorage::class
    singleOf(::AccountCleaner) bind IAccountCleaner::class
    singleOf(::EnabledWalletsStorage) bind IEnabledWalletStorage::class
    singleOf(::HardwarePublicKeyStorage) bind IHardwarePublicKeyStorage::class
    singleOf(::BlockchainSettingsStorage)
    singleOf(::SpamAddressStorage)
    factoryOf(::SwapProviderTransactionsStorage)
    factoryOf(::SwapTransactionMatcher)
    singleOf(::EvmSyncSourceStorage)
    singleOf(::ContactsRepository)
    singleOf(::ZcashBirthdayProvider)
    singleOf(::EvmLabelProvider)
    singleOf(::FeeRateProvider)

    factoryOf(::EvmAccountManagerFactory)
    singleOf(::AdapterFactory)
    factoryOf(::TransactionViewItemFactory)

    factoryOf(::TransactionRecordRepository) bind ITransactionRecordRepository::class
    factoryOf(::AddressCheckerControlImpl) bind AddressCheckerControl::class

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
    single { get<AppDatabase>().evmAddressLabelDao() }
    single { get<AppDatabase>().evmMethodLabelDao() }
    single { get<AppDatabase>().syncerStateDao() }
    single { get<AppDatabase>().recentAddressDao() }
    single { get<AppDatabase>().tokenAutoEnabledBlockchainDao() }
    single { get<AppDatabase>().userDeletedWalletDao() }
    singleOf(::UserDeletedWalletManager)
    singleOf(::DeletedWalletChecker) bind IDeletedWalletChecker::class
    single { get<AppDatabase>().moneroFileDao() }
    single { get<AppDatabase>().zcashSingleUseAddressDao() }
    single { get<AppDatabase>().spamAddressDao() }
    single { get<AppDatabase>().poisonAddressDao() }
    single { get<AppDatabase>().pendingMultiSwapDao() }
    single { get<AppDatabase>().pendingTransactionDao() }
    single { get<AppDatabase>().swapProviderTransactionsDao() }
    single { get<AppDatabase>().pinDao() }
    singleOf(::PendingMultiSwapStorage)
    singleOf(::PendingTransactionStorage)
    singleOf(::PinDbStorage)
    single {
        ZcashSingleUseAddressStorage(
            dao = get(),
            dispatcherProvider = get()
        )
    }
    factory { (accountId: String) ->
        ZcashSingleUseAddressManager(
            storage = get(),
            accountId = accountId
        )
    }
}

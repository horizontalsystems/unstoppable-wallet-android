package cash.p.terminal.di

import androidx.preference.PreferenceManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.AdapterManager
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.DefaultCurrencyManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.managers.LocalStorageManager
import cash.p.terminal.core.managers.MoneroKitManager
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.managers.SolanaWalletManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.managers.TonKitManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.managers.TronKitManager
import cash.p.terminal.modules.transactions.TransactionSyncStateRepository
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.managers.ITransactionHiddenManager
import com.m2049r.xmrwallet.service.MoneroWalletService
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.CurrencyManager
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val managerModule = module {
    singleOf(::LanguageManager)
    singleOf(::DefaultCurrencyManager) bind CurrencyManager::class
    singleOf(::SolanaRpcSourceManager)
    singleOf(::AdapterManager) bind IAdapterManager::class
    singleOf(::LocalStorageManager) bind ILocalStorage::class
    single { PreferenceManager.getDefaultSharedPreferences(get()) }
    singleOf(::BackgroundManager)
    singleOf(::EvmSyncSourceManager)
    singleOf(::TokenAutoEnableManager)
    singleOf(::EvmBlockchainManager)
    singleOf(::BtcBlockchainManager)
    singleOf(::BalanceHiddenManager)
    singleOf(::SolanaKitManager)
    singleOf(::TonKitManager)
    singleOf(::TronKitManager)
    factoryOf(::StackingManager)
    singleOf(::RestoreSettingsManager)
    singleOf(::EvmLabelManager)
    factoryOf(::SolanaWalletManager)

    singleOf(::MoneroKitManager)
    singleOf(::MoneroWalletService)

    singleOf(::TransactionAdapterManager)
    singleOf(::TransactionSyncStateRepository)
    singleOf(::BalanceHiddenManager) bind IBalanceHiddenManager::class
    singleOf(::TransactionHiddenManager) bind ITransactionHiddenManager::class
}
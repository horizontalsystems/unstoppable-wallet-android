package cash.p.terminal.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.balance.BalanceAdapterRepository
import cash.p.terminal.modules.balance.BalanceCache
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionRecordRepository
import cash.p.terminal.modules.transactions.TransactionSyncStateRepository
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionViewItemFactory
import cash.p.terminal.modules.transactions.TransactionsRateRepository

class TokenBalanceModule {

    class Factory(private val wallet: cash.p.terminal.wallet.Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val balanceService = TokenBalanceService(
                wallet,
                DefaultBalanceXRateRepository("wallet", App.currencyManager, App.marketKit),
                BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao())),
            )

            val tokenTransactionsService = TokenTransactionsService(
                wallet,
                TransactionRecordRepository(App.transactionAdapterManager),
                TransactionsRateRepository(App.currencyManager, App.marketKit),
                TransactionSyncStateRepository(App.transactionAdapterManager),
                App.contactsRepository,
                NftMetadataService(App.nftMetadataManager),
                App.spamManager
            )

            return TokenBalanceViewModel(
                wallet,
                balanceService,
                BalanceViewItemFactory(),
                tokenTransactionsService,
                TransactionViewItemFactory(App.evmLabelManager, App.contactsRepository, App.balanceHiddenManager),
                App.balanceHiddenManager,
                App.connectivityManager,
                App.accountManager,
            ) as T
        }
    }

    data class TokenBalanceUiState(
        val title: String,
        val balanceViewItem: BalanceViewItem?,
        val transactions: Map<String, List<TransactionViewItem>>?,
    )
}

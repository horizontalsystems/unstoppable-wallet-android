package cash.p.terminal.modules.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.hodler.LockTimeInterval
import java.math.BigDecimal
import java.util.Date

object TransactionsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val transactionsService = TransactionsService(
                TransactionsRateRepository(App.currencyManager, App.marketKit),
                TransactionSyncStateRepository(App.transactionAdapterManager),
                App.contactsRepository,
                NftMetadataService(App.nftMetadataManager),
                App.spamManager
            )

            return TransactionsViewModel(
                service = transactionsService,
                transactionViewItem2Factory = getKoinInstance(),
                balanceHiddenManager = App.balanceHiddenManager,
                transactionAdapterManager = App.transactionAdapterManager,
                walletManager = App.walletManager,
                transactionFilterService = TransactionFilterService(
                    App.marketKit,
                    App.transactionAdapterManager,
                    App.spamManager
                ),
                transactionHiddenManager = getKoinInstance()
            ) as T
        }
    }
}

data class TransactionLockInfo(
    val lockedUntil: Date,
    val originalAddress: String,
    val amount: BigDecimal?,
    val lockTimeInterval: LockTimeInterval
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Float) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

data class TransactionWallet(
    val token: Token?,
    val source: TransactionSource,
    val badge: String?
)

data class FilterToken(
    val token: Token,
    val source: TransactionSource,
)

package io.horizontalsystems.walletkit.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.balance.AttentionIcon
import io.horizontalsystems.walletkit.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.walletkit.modules.balance.BalanceCache
import io.horizontalsystems.walletkit.modules.balance.BalanceViewItem
import io.horizontalsystems.walletkit.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.walletkit.modules.balance.BalanceXRateRepository
import io.horizontalsystems.walletkit.modules.transactions.NftMetadataService
import io.horizontalsystems.walletkit.modules.transactions.TransactionRecordRepository
import io.horizontalsystems.walletkit.modules.transactions.TransactionSyncStateRepository
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItemFactory
import io.horizontalsystems.walletkit.modules.transactions.TransactionsRateRepository
import java.math.BigDecimal

class TokenBalanceModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val balanceService = TokenBalanceService(
                wallet,
                BalanceXRateRepository("wallet", App.currencyManager, App.marketKit),
                BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao())),
            )

            val tokenTransactionsService = TokenTransactionsService(
                wallet,
                TransactionRecordRepository(App.transactionAdapterManager),
                TransactionsRateRepository(App.currencyManager, App.marketKit),
                TransactionSyncStateRepository(App.transactionAdapterManager),
                App.contactsRepository,
                NftMetadataService(App.nftMetadataManager),
                App.spamManager,
                App.transactionAdapterManager
            )

            return TokenBalanceViewModel(
                wallet,
                balanceService,
                BalanceViewItemFactory(),
                tokenTransactionsService,
                TransactionViewItemFactory(App.evmLabelManager, App.contactsRepository, App.balanceHiddenManager, App.localStorage),
                App.balanceHiddenManager,
                App.adapterManager,
                App.connectivityManager,
                App.localStorage,
                App.coinManager,
                App.restoreSettingsManager,
            ) as T
        }
    }

    data class TokenBalanceUiState(
        val title: String,
        val balanceViewItem: BalanceViewItem?,
        val transactions: Map<String, List<TransactionViewItem>>?,
        val receiveAddress: String?,
        val error: TokenBalanceError? = null,
        val failedErrorMessage: String?,
        val warningMessage: String?,
        val alertUnshieldedBalance: BigDecimal?,
        val attentionIcon: AttentionIcon?,
        val showTronNotActiveAlert: Boolean,
        val zcashMigrationRequiredAmount: String? = null,
    )

    data class TokenBalanceError(
        val message: String,
        val errorTitle: String? = null,
        val icon: Int = R.drawable.warning_filled_24,
        val showRetryButton: Boolean = false,
        val showChangeSourceButton: Boolean = false,
    )

}

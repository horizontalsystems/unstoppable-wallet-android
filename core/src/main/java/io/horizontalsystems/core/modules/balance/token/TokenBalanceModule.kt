package io.horizontalsystems.core.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.entities.Wallet
import io.horizontalsystems.core.modules.balance.AttentionIcon
import io.horizontalsystems.core.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.core.modules.balance.BalanceCache
import io.horizontalsystems.core.modules.balance.BalanceViewItem
import io.horizontalsystems.core.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.core.modules.balance.BalanceXRateRepository
import io.horizontalsystems.core.modules.transactions.NftMetadataService
import io.horizontalsystems.core.modules.transactions.TransactionRecordRepository
import io.horizontalsystems.core.modules.transactions.TransactionSyncStateRepository
import io.horizontalsystems.core.modules.transactions.TransactionViewItem
import io.horizontalsystems.core.modules.transactions.TransactionViewItemFactory
import io.horizontalsystems.core.modules.transactions.TransactionsRateRepository
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
    )

    data class TokenBalanceError(
        val message: String,
        val errorTitle: String? = null,
        val icon: Int = R.drawable.warning_filled_24,
        val showRetryButton: Boolean = false,
        val showChangeSourceButton: Boolean = false,
    )

}

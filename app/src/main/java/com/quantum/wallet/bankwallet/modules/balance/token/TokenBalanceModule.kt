package com.quantum.wallet.bankwallet.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.balance.AttentionIcon
import com.quantum.wallet.bankwallet.modules.balance.BalanceAdapterRepository
import com.quantum.wallet.bankwallet.modules.balance.BalanceCache
import com.quantum.wallet.bankwallet.modules.balance.BalanceViewItem
import com.quantum.wallet.bankwallet.modules.balance.BalanceViewItemFactory
import com.quantum.wallet.bankwallet.modules.balance.BalanceXRateRepository
import com.quantum.wallet.bankwallet.modules.transactions.NftMetadataService
import com.quantum.wallet.bankwallet.modules.transactions.TransactionRecordRepository
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSyncStateRepository
import com.quantum.wallet.bankwallet.modules.transactions.TransactionViewItem
import com.quantum.wallet.bankwallet.modules.transactions.TransactionViewItemFactory
import com.quantum.wallet.bankwallet.modules.transactions.TransactionsRateRepository
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

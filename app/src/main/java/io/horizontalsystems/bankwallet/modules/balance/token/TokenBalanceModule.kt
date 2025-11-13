package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.transactions.NftMetadataService
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordRepository
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSyncStateRepository
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsRateRepository
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant

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
                App.spamManager
            )

            return TokenBalanceViewModel(
                wallet,
                balanceService,
                BalanceViewItemFactory(),
                tokenTransactionsService,
                TransactionViewItemFactory(App.evmLabelManager, App.contactsRepository, App.balanceHiddenManager, App.localStorage),
                App.balanceHiddenManager,
                App.accountManager,
                App.adapterManager,
                App.connectivityManager,
            ) as T
        }
    }

    data class TokenBalanceUiState(
        val title: String,
        val balanceViewItem: BalanceViewItem?,
        val transactions: Map<String, List<TransactionViewItem>>?,
        val receiveAddress: String?,
        val failedIconVisible: Boolean,
        val error: TokenBalanceError? = null,
        val failedErrorMessage: String?,
        val warningMessage: String?,
    )

    data class TokenBalanceError(
        val message: String,
        val errorTitle: String? = null,
        val showRetryButton: Boolean = false,
        val showChangeSourceButton: Boolean = false,
    )

    data class ButtonAction(
        val title: String,
        val buttonVariant: ButtonVariant = ButtonVariant.Secondary,
        val onClick: () -> Unit
    )

    data class BottomSheetContent(
        val icon: Int,
        val title: String,
        val description: String,
        val buttons: List<ButtonAction> = emptyList()
    )

}

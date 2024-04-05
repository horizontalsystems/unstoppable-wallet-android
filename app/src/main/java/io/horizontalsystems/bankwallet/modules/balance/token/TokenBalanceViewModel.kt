package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.token.TokenBalanceModule.TokenBalanceUiState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItemFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class TokenBalanceViewModel(
    private val wallet: Wallet,
    private val balanceService: TokenBalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val transactionsService: TokenTransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val connectivityManager: ConnectivityManager,
    private val accountManager: IAccountManager
) : ViewModelUiState<TokenBalanceUiState>() {

    private val title = wallet.token.coin.code + wallet.token.badge?.let { " ($it)" }.orEmpty()

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            balanceService.balanceItemFlow.collect { balanceItem ->
                balanceItem?.let {
                    updateBalanceViewItem(it)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                balanceService.balanceItem?.let {
                    updateBalanceViewItem(it)
                    transactionViewItem2Factory.updateCache()
                    transactionsService.refreshList()
                }
            }
        }

        viewModelScope.launch {
            transactionsService.itemsObservable.asFlow().collect {
                updateTransactions(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            balanceService.start()
            delay(300)
            transactionsService.start()
        }
    }

    override fun createState() = TokenBalanceUiState(
        title = title,
        balanceViewItem = balanceViewItem,
        transactions = transactions,
    )

    private fun updateTransactions(items: List<TransactionItem>) {
        transactions = items
            .map { transactionViewItem2Factory.convertToViewItemCached(it) }
            .groupBy { it.formattedDate }

        emitState()
    }

    private fun updateBalanceViewItem(balanceItem: BalanceModule.BalanceItem) {
        val balanceViewItem = balanceViewItemFactory.viewItem(
            balanceItem,
            balanceService.baseCurrency,
            balanceHiddenManager.balanceHidden,
            wallet.account.isWatchAccount,
            BalanceViewType.CoinThenFiat
        )

        this.balanceViewItem = balanceViewItem.copy(
            primaryValue = balanceViewItem.primaryValue.copy(value = balanceViewItem.primaryValue.value + " " + balanceViewItem.coinCode)
        )

        emitState()
    }

    @Throws(BackupRequiredError::class, IllegalStateException::class)
    fun getWalletForReceive(): Wallet {
        val account = accountManager.activeAccount ?: throw IllegalStateException("Active account is not set")
        when {
            account.hasAnyBackup -> return wallet
            else -> throw BackupRequiredError(account, wallet.coin.name)
        }
    }

    fun onBottomReached() {
        transactionsService.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        transactionsService.fetchRateIfNeeded(viewItem.uid)
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = transactionsService.getTransactionItem(viewItem.uid)

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): BalanceViewModel.SyncError = when {
        connectivityManager.isConnected -> BalanceViewModel.SyncError.Dialog(viewItem.wallet, viewItem.errorMessage)
        else -> BalanceViewModel.SyncError.NetworkNotAvailable()
    }

    override fun onCleared() {
        super.onCleared()

        balanceService.clear()
    }

}

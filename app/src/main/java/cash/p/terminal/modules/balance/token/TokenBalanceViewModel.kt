package cash.p.terminal.modules.balance.token

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.domain.usecase.UpdateChangeNowStatusesUseCase
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.modules.balance.BackupRequiredError
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.balance.token.TokenBalanceModule.TokenBalanceUiState
import cash.p.terminal.modules.transactions.TransactionItem
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionViewItemFactory
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
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
    private val accountManager: IAccountManager,
    private val transactionHiddenManager: TransactionHiddenManager,
    private val getChangeNowAssociatedCoinTickerUseCase: GetChangeNowAssociatedCoinTickerUseCase
) : ViewModelUiState<TokenBalanceUiState>() {

    private val updateChangeNowStatusesUseCase: UpdateChangeNowStatusesUseCase = getKoinInstance()
    private val adapterManager: IAdapterManager = getKoinInstance()

    private val title = wallet.token.coin.code + wallet.token.badge?.let { " ($it)" }.orEmpty()

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var hasHiddenTransactions: Boolean = false

    private var statusCheckerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            balanceService.balanceItemFlow.collect { balanceItem ->
                balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                balanceService.balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
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

        viewModelScope.launch {
            transactionHiddenManager.transactionHiddenFlow.collectLatest {
                transactionsService.refreshList()
            }
        }
    }

    private suspend fun isSwappable(token: Token) =
        App.instance.isSwapEnabled &&
                getChangeNowAssociatedCoinTickerUseCase(
                    token.coin.uid,
                    token.blockchainType.uid
                ) != null

    fun showAllTransactions(show: Boolean) = transactionHiddenManager.showAllTransactions(show)

    fun startStatusChecker() {
        statusCheckerJob?.cancel()
        statusCheckerJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                adapterManager.getReceiveAdapterForWallet(wallet)?.let { adapter ->
                    if (updateChangeNowStatusesUseCase(wallet.token, adapter.receiveAddress)) {
                        transactionsService.refreshList()
                    }
                }
                delay(30_000) // update status every 30 seconds
            }
        }
    }

    fun stopStatusChecker() {
        statusCheckerJob?.cancel()
    }

    override fun createState() = TokenBalanceUiState(
        title = title,
        balanceViewItem = balanceViewItem,
        transactions = transactions,
        hasHiddenTransactions = hasHiddenTransactions
    )

    private fun updateTransactions(items: List<TransactionItem>) {
        transactions =
            if (transactionHiddenManager.transactionHiddenFlow.value.transactionHidden) {
                when (transactionHiddenManager.transactionHiddenFlow.value.transactionDisplayLevel) {
                    TransactionDisplayLevel.NOTHING -> emptyList()
                    TransactionDisplayLevel.LAST_1_TRANSACTION -> items.take(1)
                    TransactionDisplayLevel.LAST_2_TRANSACTIONS -> items.take(2)
                    TransactionDisplayLevel.LAST_4_TRANSACTIONS -> items.take(4)
                }.also { hasHiddenTransactions = items.size != it.size }
            } else {
                items.also { hasHiddenTransactions = false }
            }.map { transactionViewItem2Factory.convertToViewItemCached(it) }
                .groupBy { it.formattedDate }

        emitState()
    }

    private fun updateBalanceViewItem(balanceItem: BalanceItem, isSwappable: Boolean) {
        val balanceViewItem = balanceViewItemFactory.viewItem(
            item = balanceItem,
            currency = balanceService.baseCurrency,
            hideBalance = balanceHiddenManager.balanceHidden,
            watchAccount = wallet.account.isWatchAccount,
            balanceViewType = BalanceViewType.CoinThenFiat,
            isSwappable = isSwappable
        )

        this.balanceViewItem = balanceViewItem.copy(
            primaryValue = balanceViewItem.primaryValue.copy(value = balanceViewItem.primaryValue.value + " " + balanceViewItem.wallet.coin.code)
        )

        emitState()
    }

    @Throws(BackupRequiredError::class, IllegalStateException::class)
    fun getWalletForReceive(): Wallet {
        val account =
            accountManager.activeAccount ?: throw IllegalStateException("Active account is not set")
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

    fun getTransactionItem(viewItem: TransactionViewItem) =
        transactionsService.getTransactionItem(viewItem.uid)?.copy(
            transactionStatusUrl = viewItem.transactionStatusUrl
        )

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): BalanceViewModel.SyncError = when {
        connectivityManager.isConnected -> BalanceViewModel.SyncError.Dialog(
            viewItem.wallet,
            viewItem.errorMessage
        )

        else -> BalanceViewModel.SyncError.NetworkNotAvailable()
    }

    override fun onCleared() {
        super.onCleared()

        balanceService.clear()
    }
}

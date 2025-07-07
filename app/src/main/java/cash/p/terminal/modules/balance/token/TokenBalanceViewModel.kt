package cash.p.terminal.modules.balance.token

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.core.usecase.UpdateChangeNowStatusesUseCase
import cash.p.terminal.modules.balance.BackupRequiredError
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService
import cash.p.terminal.modules.balance.token.TokenBalanceModule.TokenBalanceUiState
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
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
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.Executors

class TokenBalanceViewModel(
    private val totalBalance: TotalBalance,
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

    private val logger = AppLogger("TokenBalanceViewModel-${wallet.coin.code}")
    private val updateChangeNowStatusesUseCase: UpdateChangeNowStatusesUseCase = getKoinInstance()
    private val adapterManager: IAdapterManager = getKoinInstance()

    private val title = wallet.token.coin.code + wallet.token.badge?.let { " ($it)" }.orEmpty()

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var hasHiddenTransactions: Boolean = false

    private var statusCheckerJob: Job? = null
    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    var secondaryValue by mutableStateOf<DeemedValue<String>>(DeemedValue<String>(""))
        private set

    var refreshing by mutableStateOf<Boolean>(false)
        private set

    private var showCurrencyAsSecondary = true
    private val dedicatedDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    init {
        viewModelScope.launch(dedicatedDispatcher) {
            balanceService.balanceItemFlow.collect { balanceItem ->
                balanceItem?.let {
                    updateBalanceViewItem(
                        balanceItem = it,
                        isSwappable = isSwappable(it.wallet.token)
                    )
                }
            }
        }

        viewModelScope.launch(dedicatedDispatcher) {
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

        viewModelScope.launch(dedicatedDispatcher) {
            transactionsService.itemsObservable.asFlow().collect {
                updateTransactions(it)
            }
        }

        viewModelScope.launch(dedicatedDispatcher) {
            balanceService.start()
//            delay(300)
            transactionsService.start()
        }

        viewModelScope.launch(dedicatedDispatcher) {
            transactionHiddenManager.transactionHiddenFlow.collectLatest {
                transactionsService.refreshList()
            }
        }

        viewModelScope.launch(dedicatedDispatcher) {
            totalBalance.stateFlow.collectLatest { totalBalanceValue ->
                updateSecondaryValue(totalBalanceValue)
            }
        }

        totalBalance.start(viewModelScope)
    }

    private fun updateSecondaryValue(totalBalanceValue: TotalService.State = totalBalance.stateFlow.value) {
        if (totalBalanceValue is TotalService.State.Visible) {
            // Check if the current secondary value is the same as the wallet's coin and switch to next
            if (!showCurrencyAsSecondary && totalBalanceValue.coinValue?.coin?.uid == wallet.coin.uid) {
                toggleTotalType()
                return
            }
            balanceViewItem?.let { oldBalanceViewItem ->
                secondaryValue = DeemedValue(
                    value = if (showCurrencyAsSecondary) {
                        totalBalanceValue.currencyValue?.getFormattedFull().orEmpty()
                    } else {
                        totalBalanceValue.coinValue?.getFormattedFull().orEmpty()
                    },
                    dimmed = oldBalanceViewItem.secondaryValue.dimmed,
                    visible = oldBalanceViewItem.secondaryValue.visible
                )
            }
        }
    }

    private fun isBep20(token: Token) =
        token.type is TokenType.Eip20 && token.blockchainType == BlockchainType.BinanceSmartChain

    private suspend fun isSwappable(token: Token) =
        App.instance.isSwapEnabled && (
                isBep20(token) ||
                        getChangeNowAssociatedCoinTickerUseCase(
                            token.coin.uid,
                            token.blockchainType.uid
                        ) != null)

    fun showAllTransactions(show: Boolean) = transactionHiddenManager.showAllTransactions(show)

    fun startStatusChecker() {
        statusCheckerJob?.cancel()
        statusCheckerJob = viewModelScope.launch(dedicatedDispatcher) {
            while (isActive) {
                adapterManager.getReceiveAdapterForWallet(wallet)?.let { adapter ->
                    if (updateChangeNowStatusesUseCase(wallet.token, adapter.receiveAddress)) {
                        transactionsService.refreshList(true)
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
            }.distinctBy { it.record.uid }
                .map { transactionViewItem2Factory.convertToViewItemCached(it) }
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

        totalBalance.setTotalServiceItems(
            listOf(
                TotalService.BalanceItem(
                    value = balanceItem.balanceData.total,
                    coinPrice = balanceItem.coinPrice,
                    isValuePending = false
                )
            )
        )

        emitState()
    }

    @Throws(BackupRequiredError::class, IllegalStateException::class)
    fun getWalletForReceive(): Wallet {
        val account =
            accountManager.activeAccount ?: throw IllegalStateException("Active account is not set")
        when {
            account.hasAnyBackup || !wallet.account.accountSupportsBackup -> return wallet
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
            transactionStatusUrl = viewItem.transactionStatusUrl,
            changeNowTransactionId = viewItem.changeNowTransactionId
        )

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    fun toggleTotalType() {
        val currentSecondaryToken = totalBalance.stateFlow.value as? TotalService.State.Visible
        if (showCurrencyAsSecondary) {
            showCurrencyAsSecondary = false
            updateSecondaryValue()
            return
        } else if (currentSecondaryToken?.coinValue?.coin?.uid == BlockchainType.Bitcoin.uid) {
            showCurrencyAsSecondary = true
            updateSecondaryValue()
        }
        totalBalance.toggleTotalType()
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): BalanceViewModel.SyncError = when {
        connectivityManager.isConnected -> BalanceViewModel.SyncError.Dialog(
            viewItem.wallet,
            viewItem.errorMessage
        )

        else -> BalanceViewModel.SyncError.NetworkNotAvailable()
    }

    fun proposeShielding() {
        val logger = logger.getScopedUnique()
        viewModelScope.launch {
            try {
                sendResult = SendResult.Sending
                (adapterManager.getAdapterForWallet(wallet) as? ZcashAdapter?)?.let { adapter ->
                    adapter.proposeShielding()
                }
                sendResult = SendResult.Sent()
            } catch (e: Throwable) {
                logger.warning("failed", e)
                sendResult = SendResult.Failed(SendZCashViewModel.createCaution(e))
            }
            delay(1000)
            sendResult = null
        }
    }

    fun refresh() = viewModelScope.launch {
        refreshing = true
        adapterManager.refreshByWallet(wallet)
        delay(1000) // to show refresh indicator because `refreshByWallet` works asynchronously
        refreshing = false
    }

    override fun onCleared() {
        super.onCleared()

        balanceService.clear()
        totalBalance.stop()
        dedicatedDispatcher.close()
    }
}

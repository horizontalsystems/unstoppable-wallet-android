package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
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
    private val accountManager: IAccountManager,
    private val adapterManager: IAdapterManager,
) : ViewModelUiState<TokenBalanceUiState>() {

    private val title = wallet.token.coin.code + wallet.token.badge?.let { " ($it)" }.orEmpty()

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var addressForWatchAccount: String? = null
    private var error: TokenBalanceModule.TokenBalanceError? = null
    private var failedIconVisible = false
    private var loadingTransactions = true

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
        receiveAddressForWatchAccount = addressForWatchAccount,
        failedIconVisible = failedIconVisible,
        error = error
    )

    private fun setReceiveAddressForWatchAccount() {
        addressForWatchAccount = adapterManager.getReceiveAdapterForWallet(wallet)?.receiveAddress
        emitState()
    }

    private fun updateTransactions(items: List<TransactionItem>) {
        transactions = items
            .map { transactionViewItem2Factory.convertToViewItemCached(it) }
            .groupBy { it.formattedDate }

        loadingTransactions = false
        updateErrorState()
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

        failedIconVisible = balanceViewItem.failedIconVisible

        if (wallet.account.isWatchAccount) {
            setReceiveAddressForWatchAccount()
        }

        this.balanceViewItem = balanceViewItem.copy(
            primaryValue = balanceViewItem.primaryValue.copy(value = balanceViewItem.primaryValue.value + " " + balanceViewItem.wallet.coin.code)
        )

        updateErrorState()
        emitState()
    }

    private fun updateErrorState() {
        if (!loadingTransactions && transactions.isNullOrEmpty()) {
            error = if (balanceViewItem?.syncingProgress?.progress != null) {
                TokenBalanceModule.TokenBalanceError(
                    message = Translator.getString(R.string.Transactions_WaitForSync),
                )
            } else if (balanceViewItem?.warning != null) {
                balanceViewItem?.warning?.let{
                    TokenBalanceModule.TokenBalanceError(
                        message = it.text.toString(),
                        errorTitle = it.title.toString()
                    )
                }
            } else {
                TokenBalanceModule.TokenBalanceError(
                    message = Translator.getString(R.string.Transactions_EmptyList)
                )
            }
        } else {
            error = null
        }
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
        transactionsService.getTransactionItem(viewItem.uid)

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    override fun onCleared() {
        super.onCleared()

        balanceService.clear()
    }

}

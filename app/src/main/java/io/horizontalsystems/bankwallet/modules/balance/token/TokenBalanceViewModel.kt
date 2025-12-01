package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
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
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class TokenBalanceViewModel(
    val wallet: Wallet,
    private val balanceService: TokenBalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val transactionsService: TokenTransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val accountManager: IAccountManager,
    private val adapterManager: IAdapterManager,
    private val connectivityManager: ConnectivityManager,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<TokenBalanceUiState>() {

    private val title = wallet.token.coin.code + wallet.token.badge?.let { " ($it)" }.orEmpty()

    private var balanceViewItem: BalanceViewItem? = null
    private var transactions: Map<String, List<TransactionViewItem>>? = null
    private var addressForAccount: String? = null
    private var error: TokenBalanceModule.TokenBalanceError? = null
    private var failedIconVisible = false
    private var failedErrorMessage: String? = null
    private var waringMessage: String? = null
    private var loadingTransactions = true
    private var alertUnshieldedBalance: BigDecimal? = null

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

        if (wallet.account.type is AccountType.MoneroWatchAccount) {
            waringMessage = Translator.getString(R.string.Watch_Monero_Warning)
        }
    }

    override fun createState() = TokenBalanceUiState(
        title = title,
        balanceViewItem = balanceViewItem,
        transactions = transactions,
        receiveAddress = addressForAccount,
        failedIconVisible = failedIconVisible,
        failedErrorMessage = failedErrorMessage,
        error = error,
        warningMessage = waringMessage,
        alertUnshieldedBalance = alertUnshieldedBalance,
    )

    private fun setReceiveAddressForWatchAccount() {
        addressForAccount = adapterManager.getReceiveAdapterForWallet(wallet)?.receiveAddress
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
            BalanceViewType.CoinThenFiat,
            connectivityManager.isConnected
        )

        failedIconVisible = balanceViewItem.failedIconVisible
        failedErrorMessage = balanceViewItem.errorMessage

        if (wallet.account.isWatchAccount) {
            setReceiveAddressForWatchAccount()
        }

        if (wallet.token.blockchainType == BlockchainType.Zcash) {
            handleZcashBalanceUpdate(balanceItem)
        }

        this.balanceViewItem = balanceViewItem.copy(
            primaryValue = balanceViewItem.primaryValue?.let {
                it.copy(value = it.value + " " + balanceViewItem.wallet.coin.code)
            }
        )

        updateErrorState()
        emitState()
    }

    private fun handleZcashBalanceUpdate(balanceItem: BalanceModule.BalanceItem) {
        if (balanceItem.state == AdapterState.Synced && balanceItem.balanceData.unshielded > ZcashAdapter.minimalShieldThreshold) {
            val unshielded = balanceItem.balanceData.unshielded
            val lastAlertedUnshieldedBalance = getLastAlertedUnshieldedBalance(wallet)

            if (lastAlertedUnshieldedBalance == null || lastAlertedUnshieldedBalance.compareTo(unshielded) != 0) {
                alertUnshieldedBalance = unshielded
            }
        }
    }

    private fun getLastAlertedUnshieldedBalance(wallet: Wallet) : BigDecimal? {
        return localStorage.zcashUnshieldedBalanceAlerts.get(wallet.account.id)
    }

    private fun setLastAlertedUnshieldedBalance(
        wallet: Wallet,
        unshielded: BigDecimal
    ) {
        localStorage.zcashUnshieldedBalanceAlerts = localStorage.zcashUnshieldedBalanceAlerts + mapOf(wallet.account.id to unshielded)
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

    fun transparentZecAmountWarningShown(alertUnshieldedBalance: BigDecimal) {
        setLastAlertedUnshieldedBalance(wallet, alertUnshieldedBalance)
        this.alertUnshieldedBalance = null

        emitState()
    }

    override fun onCleared() {
        super.onCleared()

        balanceService.clear()
    }

}

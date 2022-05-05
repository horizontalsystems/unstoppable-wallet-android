package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val totalService: TotalService
) : ViewModel() {

    private var totalState = totalService.stateFlow.value

    var uiState by mutableStateOf(
        BalanceUiState(
            totalCurrencyValue = totalState.currencyValue,
            totalCoinValue = totalState.coinValue,
            totalDimmed = totalState.dimmed,
        )
    )
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set
    val sortTypes = listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    var balanceViewItems by mutableStateOf<List<BalanceViewItem>?>(null)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var sortType by service::sortType

    private var expandedWallet: Wallet? = null

    init {
        viewModelScope.launch {
            service.balanceItemsFlow
                .collect { items ->
                    totalService.setBalanceItems(items)
                    items?.let { refreshViewItems(it) }
                }
        }
        totalService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedTotalState(it)
        }
        viewModelScope.launch {
            totalService.start()
        }

        service.start()
    }

    private fun handleUpdatedTotalState(totalState: TotalService.State) {
        this.totalState = totalState

        emitState()
    }

    private fun emitState() {
        val newUiState = BalanceUiState(
            totalCurrencyValue = totalState.currencyValue,
            totalCoinValue = totalState.coinValue,
            totalDimmed = totalState.dimmed,
        )

        viewModelScope.launch {
            uiState = newUiState
        }
    }


    private fun refreshViewItems(balanceItems: List<BalanceModule.BalanceItem>) {
        val balanceViewItems = balanceItems.map { balanceItem ->
            balanceViewItemFactory.viewItem(
                balanceItem,
                service.baseCurrency,
                balanceItem.wallet == expandedWallet,
                service.balanceHidden,
                service.isWatchAccount
            )
        }

        viewModelScope.launch {
            viewState = ViewState.Success
            this@BalanceViewModel.balanceViewItems = balanceViewItems
        }
    }


    override fun onCleared() {
        service.clear()
    }

    fun onBalanceClick() {
        service.balanceHidden = !service.balanceHidden

        service.balanceItemsFlow.value?.let { refreshViewItems(it) }
    }

    fun toggleTotalType() {
        totalService.toggleType()
    }

    fun onItem(viewItem: BalanceViewItem) {
        expandedWallet = when {
            viewItem.wallet == expandedWallet -> null
            else -> viewItem.wallet
        }

        service.balanceItemsFlow.value?.let { refreshViewItems(it) }
    }

    fun getWalletForReceive(viewItem: BalanceViewItem) = when {
        viewItem.wallet.account.isBackedUp -> viewItem.wallet
        else -> throw BackupRequiredError(viewItem.wallet.account, viewItem.coinTitle)
    }

    fun onRefresh() {
        if (isRefreshing) {
            return
        }

        viewModelScope.launch {
            isRefreshing = true
            service.refresh()
            // A fake 2 seconds 'refresh'
            delay(2300)
            isRefreshing = false
        }
    }

    fun disable(viewItem: BalanceViewItem) {
        service.disable(viewItem.wallet)
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
        service.networkAvailable -> SyncError.Dialog(viewItem.wallet, viewItem.errorMessage)
        else -> SyncError.NetworkNotAvailable()
    }

    sealed class SyncError {
        class NetworkNotAvailable : SyncError()
        class Dialog(val wallet: Wallet, val errorMessage: String?) : SyncError()
    }
}

class BackupRequiredError(val account: Account, val coinTitle: String) : Error("Backup Required")

data class BalanceUiState(
    val totalCurrencyValue: CurrencyValue?,
    val totalCoinValue: CoinValue?,
    val totalDimmed: Boolean
)

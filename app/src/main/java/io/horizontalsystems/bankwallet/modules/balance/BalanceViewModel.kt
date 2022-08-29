package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BalanceViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val totalService: TotalService,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val balanceHiddenManager: BalanceHiddenManager
) : ViewModel() {
    private var totalState = createTotalUIState(totalService.stateFlow.value)
    private var viewState: ViewState = ViewState.Loading
    private var balanceViewItems = listOf<BalanceViewItem>()
    private var isRefreshing = false
    private var balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value

    var uiState by mutableStateOf(
        BalanceUiState(
            balanceViewItems = balanceViewItems,
            viewState = viewState,
            isRefreshing = isRefreshing,
            totalState = totalState
        )
    )
        private set

    val sortTypes = listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    var sortType by service::sortType

    private var expandedWallet: Wallet? = null

    init {
        viewModelScope.launch {
            service.balanceItemsFlow
                .collect { items ->
                    totalService.setItems(items?.map {
                        TotalService.BalanceItem(
                            it.balanceData.total,
                            it.state !is AdapterState.Synced,
                            it.coinPrice
                        )
                    })
                    items?.let { refreshViewItems(it) }
                }
        }

        viewModelScope.launch {
            totalService.stateFlow.collect {
                handleUpdatedTotalState(it)
            }
        }

        viewModelScope.launch {
            balanceViewTypeManager.balanceViewTypeFlow.collect {
                handleUpdatedBalanceViewType(it)
            }
        }

        totalService.start()

        service.start()
    }

    private suspend fun handleUpdatedBalanceViewType(balanceViewType: BalanceViewType) {
        this.balanceViewType = balanceViewType

        service.balanceItemsFlow.value?.let {
            refreshViewItems(it)
        }
    }

    private suspend fun handleUpdatedTotalState(totalState: TotalService.State) {
        withContext(Dispatchers.IO) {
            this@BalanceViewModel.totalState = createTotalUIState(totalState)

            emitState()
        }
    }

    private fun emitState() {
        val newUiState = BalanceUiState(
            balanceViewItems = balanceViewItems,
            viewState = viewState,
            isRefreshing = isRefreshing,
            totalState = totalState
        )

        viewModelScope.launch {
            uiState = newUiState
        }
    }


    private suspend fun refreshViewItems(balanceItems: List<BalanceModule.BalanceItem>) {
        withContext(Dispatchers.IO) {
            viewState = ViewState.Success

            balanceViewItems = balanceItems.map { balanceItem ->
                balanceViewItemFactory.viewItem(
                    balanceItem,
                    service.baseCurrency,
                    balanceItem.wallet == expandedWallet,
                    balanceHiddenManager.balanceHidden,
                    service.isWatchAccount,
                    balanceViewType
                )
            }

            emitState()
        }
    }

    override fun onCleared() {
        totalService.stop()
        service.clear()
    }

    fun onBalanceClick() {
        viewModelScope.launch {
            balanceHiddenManager.toggleBalanceHidden()
            service.balanceItemsFlow.value?.let { refreshViewItems(it) }
        }
    }

    fun toggleTotalType() {
        totalService.toggleType()
    }

    fun onItem(viewItem: BalanceViewItem) {
        viewModelScope.launch {
            expandedWallet = when {
                viewItem.wallet == expandedWallet -> null
                else -> viewItem.wallet
            }

            service.balanceItemsFlow.value?.let { refreshViewItems(it) }
        }
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
            emitState()

            service.refresh()
            // A fake 2 seconds 'refresh'
            delay(2300)

            isRefreshing = false
            emitState()
        }
    }

    fun disable(viewItem: BalanceViewItem) {
        service.disable(viewItem.wallet)
    }

    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
        service.networkAvailable -> SyncError.Dialog(viewItem.wallet, viewItem.errorMessage)
        else -> SyncError.NetworkNotAvailable()
    }

    private fun createTotalUIState(totalState: TotalService.State) = when (totalState) {
        TotalService.State.Hidden -> TotalUIState.Hidden
        is TotalService.State.Visible -> TotalUIState.Visible(
            currencyValueStr = totalState.currencyValue?.let {
                App.numberFormatter.formatFiatFull(it.value, it.currency.symbol)
            } ?: "---",
            coinValueStr = totalState.coinValue?.let {
                "~" + App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
            } ?: "---",
            dimmed = totalState.dimmed
        )
    }

    sealed class SyncError {
        class NetworkNotAvailable : SyncError()
        class Dialog(val wallet: Wallet, val errorMessage: String?) : SyncError()
    }
}

class BackupRequiredError(val account: Account, val coinTitle: String) : Error("Backup Required")

data class BalanceUiState(
    val balanceViewItems: List<BalanceViewItem>,
    val viewState: ViewState,
    val isRefreshing: Boolean,
    val totalState: TotalUIState
)

sealed class TotalUIState {
    data class Visible(
        val currencyValueStr: String,
        val coinValueStr: String,
        val dimmed: Boolean
    ) : TotalUIState()

    object Hidden : TotalUIState()

}
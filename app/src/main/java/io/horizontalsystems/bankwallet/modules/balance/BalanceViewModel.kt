package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory
) : ViewModel() {

    val sortTypes = listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    var balanceViewItemsWrapper by mutableStateOf<Pair<BalanceHeaderViewItem, List<BalanceViewItem>>?>(null)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var sortType by service::sortType

    private var expandedWallet: Wallet? = null
    private val disposables = CompositeDisposable()

    init {
        service.balanceItemsObservable
            .subscribeIO {
                refreshViewItems()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun refreshViewItems() {
        val items = service.balanceItems.map { balanceItem ->
            balanceViewItemFactory.viewItem(
                balanceItem,
                service.baseCurrency,
                balanceItem.wallet == expandedWallet,
                service.balanceHidden,
                service.isWatchAccount
            )
        }

        val headerViewItem = balanceViewItemFactory.headerViewItem(
            service.balanceItems,
            service.baseCurrency,
            service.balanceHidden
        )

        viewModelScope.launch {
            balanceViewItemsWrapper = Pair(headerViewItem, items)
        }
    }


    override fun onCleared() {
        service.clear()
    }

    fun onBalanceClick() {
        service.balanceHidden = !service.balanceHidden

        refreshViewItems()
    }

    fun onItem(viewItem: BalanceViewItem) {
        expandedWallet = when {
            viewItem.wallet == expandedWallet -> null
            else -> viewItem.wallet
        }

        refreshViewItems()
    }

    fun getWalletForReceive(viewItem: BalanceViewItem) = when {
        viewItem.wallet.account.isBackedUp -> viewItem.wallet
        else -> throw BackupRequiredError(viewItem.wallet.account)
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

//    fun disable(viewItem: BalanceViewItem) {
//        service.disable(viewItem.wallet)
//    }
//
//    fun enable(wallet: Wallet) {
//        service.enable(wallet)
//    }
//
    fun getSyncErrorDetails(viewItem: BalanceViewItem): SyncError = when {
        service.networkAvailable -> SyncError.Dialog(viewItem.wallet, viewItem.errorMessage)
        else -> SyncError.NetworkNotAvailable()
    }

    sealed class SyncError {
        class NetworkNotAvailable : SyncError()
        class Dialog(val wallet: Wallet, val errorMessage: String?) : SyncError()
    }
}

class BackupRequiredError(val account: Account) : Error("Backup Required")

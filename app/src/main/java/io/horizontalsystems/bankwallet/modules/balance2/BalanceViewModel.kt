package io.horizontalsystems.bankwallet.modules.balance2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceHeaderViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceService2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val service: BalanceService2,
    private val balanceViewItemFactory: BalanceViewItemFactory
) : ViewModel() {

    var balanceViewItemsWrapper by mutableStateOf<Pair<BalanceHeaderViewItem, List<BalanceViewItem>>?>(null)
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
                service.balanceHidden
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
}
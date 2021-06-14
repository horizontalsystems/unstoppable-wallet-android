package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable

class BalanceViewModel2(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory
) : ViewModel() {

    val titleLiveData = service.activeAccountObservable
        .map { it.name }
        .let {
            LiveDataReactiveStreams.fromPublisher(it)
        }

    val balanceViewItemsLiveData = Flowable
        .combineLatest(
            service.balanceItemsObservable,
            service.balanceHiddenObservable,
            service.expandedWalletObservable,
        ) { items, balanceHidden, expandedWallet ->
            items.map { balanceItem ->
                balanceViewItemFactory.viewItem(
                    balanceItem,
                    service.baseCurrency,
                    balanceItem.wallet == expandedWallet.orElse(null),
                    balanceHidden
                )
            }
        }
        .let {
            LiveDataReactiveStreams.fromPublisher(it)
        }

    val headerViewItemLiveData = Flowable
        .combineLatest(
            service.balanceItemsObservable,
            service.balanceHiddenObservable
        ) { items: List<BalanceModule.BalanceItem>, balanceHidden: Boolean ->
            balanceViewItemFactory.headerViewItem(items, service.baseCurrency, balanceHidden)
        }
        .let {
            LiveDataReactiveStreams.fromPublisher(it)
        }


    private var disposables = CompositeDisposable()

    var sortType: BalanceSortType
        get() = service.sortType
        set(value) {
            service.sortType = value
        }

    init {

    }

    fun onRefresh() {
        service.refresh()
    }

    fun onBalanceClick() {
        service.toggleBalanceVisibility()
    }

    fun onItem(viewItem: BalanceViewItem) {
        service.toggleExpanded(viewItem.wallet)
    }

    fun onSyncErrorClick(viewItem: BalanceViewItem) {
        TODO("Not yet implemented")
    }

    fun onResume() {
//        TODO("Not yet implemented")
    }

    fun onPause() {
//        TODO("Not yet implemented")
    }

}

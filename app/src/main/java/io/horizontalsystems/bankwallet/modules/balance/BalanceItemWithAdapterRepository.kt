package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BalanceItemWithAdapterRepository(
    private val itemRepository: ItemRepository<BalanceModule.BalanceItem>,
    private val adapterManager: IAdapterManager
) : ItemRepository<BalanceModule.BalanceItem> {

    private var balanceItems = listOf<BalanceModule.BalanceItem>()
    private val disposables = CompositeDisposable()
    private val adaptersDisposables = CompositeDisposable()

    private val itemsSubject = BehaviorSubject.create<List<BalanceModule.BalanceItem>>()
    override val itemsObservable: Observable<List<BalanceModule.BalanceItem>>
        get() = itemsSubject
            .doOnSubscribe {
                subscribeForUpdates()
            }
            .doFinally {
                unsubscribeFromUpdates()
            }

    override fun refresh() {
        adapterManager.refresh()
        itemRepository.refresh()
    }

    private fun subscribeForUpdates() {
        itemRepository.itemsObservable
            .subscribeIO {
                balanceItems = it

                reset()
            }
            .let {
                disposables.add(it)
            }

        adapterManager.adaptersReadyObservable
            .subscribeIO {
                reset()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun reset() {
        unsubscribeFromAdapterUpdates()
        setDataFromAdapters()
        emitBalanceItems()
        subscribeForAdapterUpdates()
    }

    private fun emitBalanceItems() {
        itemsSubject.onNext(balanceItems)
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
        adaptersDisposables.clear()
    }


    private fun unsubscribeFromAdapterUpdates() {
        adaptersDisposables.clear()
    }

    private fun setDataFromAdapters() {
        for (balanceItem in balanceItems) {
            val adapter = adapterManager.getBalanceAdapterForWallet(balanceItem.wallet) ?: continue

            balanceItem.balance = adapter.balance
            balanceItem.balanceLocked = adapter.balanceLocked
            balanceItem.state = adapter.balanceState
        }
    }

    private fun subscribeForAdapterUpdates() {
        for (balanceItem in balanceItems) {
            val adapter = adapterManager.getBalanceAdapterForWallet(balanceItem.wallet) ?: continue

            subscribeForBalanceUpdate(adapter, balanceItem)
            subscribeForStateUpdate(adapter, balanceItem)
        }
    }

    private fun subscribeForBalanceUpdate(adapter: IBalanceAdapter, balanceItem: BalanceModule.BalanceItem) {
        adapter.balanceUpdatedFlowable
            .subscribeIO {
                balanceItems.find { it == balanceItem }?.apply {
                    this.balance = adapter.balance
                    this.balanceLocked = adapter.balanceLocked
                }

                emitBalanceItems()
            }
            .let {
                adaptersDisposables.add(it)
            }
    }

    private fun subscribeForStateUpdate(adapter: IBalanceAdapter, balanceItem: BalanceModule.BalanceItem) {
        adapter.balanceStateUpdatedFlowable
            .subscribeIO {
                balanceItems.find { it == balanceItem }?.apply {
                    this.state = adapter.balanceState
                }

                emitBalanceItems()
            }
            .let {
                adaptersDisposables.add(it)
            }
    }
}

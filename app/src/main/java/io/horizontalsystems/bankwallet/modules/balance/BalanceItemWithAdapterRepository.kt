package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.CopyOnWriteArrayList

class BalanceItemWithAdapterRepository(
    private val itemRepository: ItemRepository<Wallet>,
    private val adapterManager: IAdapterManager,
    private val networkTypeChecker: NetworkTypeChecker,
    private val balanceCache: BalanceCache
) : ItemRepository<BalanceModule.BalanceItem> {

    private val balanceItems = CopyOnWriteArrayList<BalanceModule.BalanceItem>()
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
            .subscribeIO { wallets ->
                balanceItems.clear()
                balanceItems.addAll(wallets.map { wallet ->
                    val (balance, balanceLocked) = balanceCache.getCache(wallet)

                    BalanceModule.BalanceItem(
                        wallet,
                        networkTypeChecker.isMainNet(wallet),
                        balance,
                        balanceLocked,
                        AdapterState.Syncing(10, null)
                    )
                })

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
        for (i in 0 until balanceItems.size) {
            val balanceItem = balanceItems[i]
            val adapter = adapterManager.getBalanceAdapterForWallet(balanceItem.wallet) ?: continue

            balanceCache.setCache(balanceItem.wallet, adapter.balance, adapter.balanceLocked)

            balanceItems[i] = balanceItem.copy(balance = adapter.balance, balanceLocked = adapter.balanceLocked, state = adapter.balanceState)
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
                val indexOfFirst = balanceItems.indexOfFirst { it.wallet == balanceItem.wallet }
                if (indexOfFirst != -1) {
                    val itemToUpdate = balanceItems[indexOfFirst]
                    balanceCache.setCache(itemToUpdate.wallet, adapter.balance, adapter.balanceLocked)

                    balanceItems[indexOfFirst] = itemToUpdate.copy(balance = adapter.balance, balanceLocked = adapter.balanceLocked)

                    emitBalanceItems()
                }
            }
            .let {
                adaptersDisposables.add(it)
            }
    }

    private fun subscribeForStateUpdate(adapter: IBalanceAdapter, balanceItem: BalanceModule.BalanceItem) {
        adapter.balanceStateUpdatedFlowable
            .subscribeIO {
                val indexOfFirst = balanceItems.indexOfFirst { it.wallet == balanceItem.wallet }
                if (indexOfFirst != -1) {
                    balanceItems[indexOfFirst] = balanceItems[indexOfFirst].copy(state = adapter.balanceState)

                    emitBalanceItems()
                }
            }
            .let {
                adaptersDisposables.add(it)
            }
    }
}

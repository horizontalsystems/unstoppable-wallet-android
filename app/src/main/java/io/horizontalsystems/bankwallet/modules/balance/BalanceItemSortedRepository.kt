package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.CopyOnWriteArrayList

class BalanceItemSortedRepository(
    private val itemRepository: ItemRepository<BalanceModule.BalanceItem>,
    private val balanceSorter: BalanceSorter,
    private val balanceConfigurator: BalanceConfigurator
) : ItemRepository<BalanceModule.BalanceItem> {

    private val balanceItems = CopyOnWriteArrayList<BalanceModule.BalanceItem>()
    private val disposables = CompositeDisposable()

    private val itemsSubject = BehaviorSubject.create<List<BalanceModule.BalanceItem>>()

    override val itemsObservable: Observable<List<BalanceModule.BalanceItem>>
        get() = itemsSubject
            .doOnSubscribe {
                subscribeForUpdates()
            }
            .doFinally {
                unsubscribeFromUpdates()
            }

    private fun subscribeForUpdates() {
        itemRepository.itemsObservable
            .subscribeIO {
                balanceItems.clear()
                balanceItems.addAll(it)

                reset()
            }
            .let {
                disposables.add(it)
            }

        balanceConfigurator.sortTypeObservable
            .subscribeIO {
                reset()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun reset() {
        val sort = balanceSorter.sort(balanceItems, balanceConfigurator.sortType)
        balanceItems.clear()
        balanceItems.addAll(sort)

        itemsSubject.onNext(balanceItems)
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    override fun refresh() = Unit
}

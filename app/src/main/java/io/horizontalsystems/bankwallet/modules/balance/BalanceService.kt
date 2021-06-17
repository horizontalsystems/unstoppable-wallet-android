package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BalanceService(
    private val adapterManager: IAdapterManager,
    private val currencyManager: ICurrencyManager,
    private val localStorage: ILocalStorage,
    private val balanceSorter: BalanceSorter,
    private val connectivityManager: ConnectivityManager,
    private val balanceItemRepository: ItemRepository<BalanceModule.BalanceItem>
) : Clearable {
    val networkAvailable: Boolean
        get() = connectivityManager.isConnected

    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            sortItems()
            emitBalanceItems()
        }

    var balanceHidden: Boolean
        get() = localStorage.balanceHidden
        set(value) {
            localStorage.balanceHidden = value
        }

    private val disposables = CompositeDisposable()

    private val balanceItemsSubject = BehaviorSubject.create<Unit>()
    val balanceItemsObservable: Flowable<Unit> = balanceItemsSubject.toFlowable(BackpressureStrategy.DROP)

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    var balanceItems = listOf<BalanceModule.BalanceItem>()
        private set

    init {
        balanceItemRepository.itemsObservable
            .subscribeIO { balanceItems ->
                this.balanceItems = balanceItems

                sortItems()
                emitBalanceItems()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun sortItems() {
        balanceItems = balanceSorter.sort(balanceItems, sortType)
    }

    private fun emitBalanceItems() {
        balanceItemsSubject.onNext(Unit)
    }

    fun refresh() {
        balanceItemRepository.refresh()
    }

    fun refreshByWallet(wallet: Wallet) {
        adapterManager.refreshByWallet(wallet)
    }

    override fun clear() {
        disposables.clear()
    }
}

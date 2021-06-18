package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BalanceService(
    private val adapterManager: IAdapterManager,
    private val currencyManager: ICurrencyManager,
    private val connectivityManager: ConnectivityManager,
    private val balanceItemRepository: ItemRepository<BalanceModule.BalanceItem>,
    private val balanceConfigurator: BalanceConfigurator
) : Clearable {

    var sortType by balanceConfigurator::sortType
    var balanceHidden by balanceConfigurator::balanceHidden

    val networkAvailable get() = connectivityManager.isConnected
    val baseCurrency get() = currencyManager.baseCurrency

    private val disposables = CompositeDisposable()

    private val balanceItemsSubject = BehaviorSubject.create<Unit>()
    val balanceItemsObservable: Observable<Unit> = balanceItemsSubject

    var balanceItems = listOf<BalanceModule.BalanceItem>()
        private set

    init {
        balanceItemRepository.itemsObservable
            .subscribeIO { balanceItems ->
                this.balanceItems = balanceItems

                emitBalanceItems()
            }
            .let {
                disposables.add(it)
            }
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

package io.horizontalsystems.bankwallet.modules.swap.allowance

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class SwapPendingAllowanceService(
        private val adapterManager: IAdapterManager,
        private val allowanceService: SwapAllowanceService
) {
    private var coin: Coin? = null
    private var pendingAllowance: BigDecimal? = null
    private val isPendingSubject = PublishSubject.create<Boolean>()

    private val disposables = CompositeDisposable()

    var isPending: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                isPendingSubject.onNext(value)
            }
        }
    val isPendingObservable: Observable<Boolean> = isPendingSubject

    init {
        allowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    sync()
                }
                .let { disposables.add(it) }
    }

    fun set(coin: Coin?) {
        this.coin = coin
        pendingAllowance = null

        syncAllowance()
    }

    fun syncAllowance() {
        val coin = coin ?: return
        val adapter = adapterManager.getAdapterForCoin(coin) as? Eip20Adapter ?: return

        adapter.pendingTransactions.forEach { transaction ->
            if (transaction.type == TransactionType.Approve) {
                pendingAllowance = transaction.amount
            }
        }

        sync()
    }

    fun onCleared() {
        disposables.clear()
    }

    private fun sync() {
        val pendingAllowance = pendingAllowance
        val allowanceState = allowanceService.state

        if (pendingAllowance == null || allowanceState == null || allowanceState !is SwapAllowanceService.State.Ready) {
            isPending = false
            return
        }

        isPending = allowanceState.allowance.value.compareTo(pendingAllowance) != 0
    }

}

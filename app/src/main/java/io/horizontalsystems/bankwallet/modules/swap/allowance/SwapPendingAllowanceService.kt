package io.horizontalsystems.bankwallet.modules.swap.allowance

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

enum class SwapPendingAllowanceState {
    NA, Pending, Approved
}

class SwapPendingAllowanceService(
    private val adapterManager: IAdapterManager,
    private val allowanceService: SwapAllowanceService
) {
    private var coin: PlatformCoin? = null
    private var pendingAllowance: BigDecimal? = null

    private val disposables = CompositeDisposable()

    private val stateSubject = PublishSubject.create<SwapPendingAllowanceState>()
    var state: SwapPendingAllowanceState = SwapPendingAllowanceState.NA
        private set(value) {
            if (field != value) {
                field = value
                stateSubject.onNext(value)
            }
        }
    val stateObservable: Observable<SwapPendingAllowanceState> = stateSubject

    init {
        allowanceService.stateObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                sync()
            }
            .let { disposables.add(it) }
    }

    fun set(coin: PlatformCoin?) {
        this.coin = coin
        pendingAllowance = null

        syncAllowance()
    }

    fun syncAllowance() {
        val coin = coin ?: return
        val adapter = adapterManager.getAdapterForPlatformCoin(coin) as? Eip20Adapter ?: return

        adapter.pendingTransactions.forEach { transaction ->
            if (transaction is ApproveTransactionRecord) {
                pendingAllowance = transaction.value.decimalValue
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
            state = SwapPendingAllowanceState.NA
            return
        }

        state = if (allowanceState.allowance.value.compareTo(pendingAllowance) != 0)
            SwapPendingAllowanceState.Pending
        else
            SwapPendingAllowanceState.Approved
    }

}

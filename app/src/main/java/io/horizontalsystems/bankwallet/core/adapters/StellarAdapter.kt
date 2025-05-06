package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StellarAdapter(
    stellarKitWrapper: StellarKitWrapper
) : BaseStellarAdapter(stellarKitWrapper), ISendStellarAdapter {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var balance: BigDecimal? = null

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceData: BalanceData
        get() = BalanceData(balance ?: BigDecimal.ZERO)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(StellarAsset.Native).collect { balance ->
                this@StellarAdapter.balance = balance
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }

    override val debugInfo = "debugInfo"

    override val availableBalance: BigDecimal
        get() = balance ?: BigDecimal.ZERO
    override val fee: BigDecimal
        get() = stellarKit.sendFee

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        stellarKit.sendNative(address, amount, memo)
    }
}
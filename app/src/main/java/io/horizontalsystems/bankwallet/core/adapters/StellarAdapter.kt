package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.Network
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal


class StellarAdapter(
    private val stellarKitWrapper: StellarKitWrapper,
) : IAdapter, IReceiveAdapter, IBalanceAdapter, ISendStellarAdapter {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val stellarKit = stellarKitWrapper.stellarKit

    override val receiveAddress = stellarKit.receiveAddress
    override val isMainNet = stellarKit.network == Network.MainNet

    private var balance = stellarKit.balance

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceData: BalanceData
        get() = BalanceData(balance)


    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun start() {
        coroutineScope.launch {
            stellarKit.balanceFlow.collect { balance ->
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
        stellarKit.stop()
    }

    override fun refresh() {
        coroutineScope.launch {
            stellarKit.refresh()
        }
    }

    override val debugInfo = "debugInfo"

    override val availableBalance: BigDecimal
        get() = balance
    override val fee: BigDecimal
        get() = stellarKit.sendFee

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        stellarKit.send(address, amount, memo)
    }
}
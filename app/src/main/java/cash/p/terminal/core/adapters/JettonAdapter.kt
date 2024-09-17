package cash.p.terminal.core.adapters

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.BalanceData
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.entities.Wallet
import io.horizontalsystems.tonkit.Address
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class JettonAdapter(tonKitWrapper: TonKitWrapper, addressStr: String, wallet: Wallet) : BaseTonAdapter(tonKitWrapper, wallet.decimal) {

    private val address = Address.parse(addressStr)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var balance = BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun start() {
        coroutineScope.launch {
            tonKit.jettonBalanceMapFlow.collect {
                val jettonBalance = it[address]
                balance = jettonBalance?.balance?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.jettonSyncStateFlow.collect {
                balanceState = convertToAdapterState(it)
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }
}

package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TonAdapter(tonKitWrapper: TonKitWrapper) : BaseTonAdapter(tonKitWrapper, 9), ISendTonAdapter {

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var balance = BigDecimal.ZERO

    override fun start() {
        coroutineScope.launch {
            tonKit.accountFlow.collect {
                balance = it?.balance?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.syncStateFlow.collect {
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

    override val debugInfo: String
        get() = ""

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val availableBalance: BigDecimal
        get() = balance

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        TODO()
//        tonKit.send(address, amount.movePointRight(decimals).toBigInteger().toString(), memo)
    }

    override suspend fun estimateFee(): BigDecimal {
        TODO()
//        return tonKit.estimateFee().toBigDecimal().movePointLeft(decimals)
    }
}

class TonTransactionRecord(
    uid: String,
    transactionHash: String,
    val logicalTime: Long,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    failed: Boolean = false,
    spam: Boolean = false,
    source: TransactionSource,
    override val mainValue: TransactionValue,
    val fee: TransactionValue?,
    val memo: String?,
    val type: Type,
    val transfers: List<TonTransactionTransfer>
) : TransactionRecord(
    uid,
    transactionHash,
    0,
    blockHeight,
    confirmationsThreshold,
    timestamp,
    failed,
    spam,
    source,
) {
    enum class Type {
        Incoming, Outgoing, Unknown
    }

    override fun status(lastBlockHeight: Int?) = TransactionStatus.Completed
}

data class TonTransactionTransfer(val src: String, val dest: String, val amount: TransactionValue.CoinValue)

package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.managers.TonKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tonkit.FriendlyAddress
import io.horizontalsystems.tonkit.core.TonKit.SendAmount
import io.horizontalsystems.tonkit.models.Account
import io.horizontalsystems.tonkit.models.Event
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.math.absoluteValue

class TonAdapter(tonKitWrapper: TonKitWrapper) : BaseTonAdapter(tonKitWrapper, 9) {

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var balance = getBalanceFromAccount(tonKit.account)

    override fun start() {
        coroutineScope.launch {
            tonKit.accountFlow.collect { account ->
                balance = getBalanceFromAccount(account)
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    private fun getBalanceFromAccount(account: Account?): BigDecimal {
        return account?.balance?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
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

    private fun getSendAmount(amount: BigDecimal) = when {
        amount.compareTo(availableBalance) == 0 -> SendAmount.Max
        else -> SendAmount.Amount(amount.movePointRight(decimals).toBigInteger())
    }

    override suspend fun send(amount: BigDecimal, address: FriendlyAddress, memo: String?) {
        tonKit.send(address, getSendAmount(amount), memo)
    }

    override suspend fun estimateFee(amount: BigDecimal, address: FriendlyAddress, memo: String?): BigDecimal {
        val estimateFee = tonKit.estimateFee(address, getSendAmount(amount), memo)
        return estimateFee.toBigDecimal(decimals).stripTrailingZeros()
    }

    companion object {
        fun getAmount(kitAmount: Long): BigDecimal {
            return kitAmount.toBigDecimal().movePointLeft(9).stripTrailingZeros()
        }
    }
}



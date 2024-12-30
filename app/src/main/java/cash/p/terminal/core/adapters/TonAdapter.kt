package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.core.ISendTonAdapter
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
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

class TonAdapter(tonKitWrapper: TonKitWrapper) : BaseTonAdapter(tonKitWrapper, 9), ISendTonAdapter {

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

class TonTransactionRecord(
    source: TransactionSource,
    event: Event,
    baseToken: Token,
    val actions: List<Action>
) : TransactionRecord(
    uid = event.id,
    transactionHash = event.id,
    transactionIndex = 0,
    blockHeight = null,
    confirmationsThreshold = null,
    timestamp = event.timestamp,
    failed = false,
    spam = event.scam,
    source = source,
) {
    val lt = event.lt
    val inProgress = event.inProgress
    val fee = TransactionValue.CoinValue(baseToken, TonAdapter.getAmount(event.extra.absoluteValue))

    override fun status(lastBlockHeight: Int?) = when {
        inProgress -> TransactionStatus.Pending
        else -> TransactionStatus.Completed
    }

    override val mainValue: TransactionValue?
        get() = actions.singleOrNull()?.let { action ->
            when (val type = action.type) {
                is Action.Type.Receive -> type.value
                is Action.Type.Send -> type.value
                is Action.Type.Burn -> type.value
                is Action.Type.Mint -> type.value
                is Action.Type.ContractCall -> type.value
                is Action.Type.ContractDeploy,
                is Action.Type.Swap,
                is Action.Type.Unsupported -> null
            }
        }

    data class Action(
        val type: Type,
        val status: TransactionStatus
    ) {
        sealed class Type {
            data class Send(
                val value: TransactionValue,
                val to: String,
                val sentToSelf: Boolean,
                val comment: String?,
            ) : Type()

            data class Receive(
                val value: TransactionValue,
                val from: String,
                val comment: String?,
            ) : Type()

            data class Burn(val value: TransactionValue) : Type()

            data class Mint(val value: TransactionValue) : Type()

            data class Swap(
                val routerName: String?,
                val routerAddress: String,
                val valueIn: TransactionValue,
                val valueOut: TransactionValue
            ) : Type()

            data class ContractDeploy(val interfaces: List<String>) : Type()

            data class ContractCall(
                val address: String,
                val value: TransactionValue,
                val operation: String
            ) : Type()

            data class Unsupported(val type: String) : Type()
        }
    }
}


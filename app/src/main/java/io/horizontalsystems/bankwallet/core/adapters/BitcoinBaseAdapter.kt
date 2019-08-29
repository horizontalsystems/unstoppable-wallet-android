package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorError
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BitcoinBaseAdapter(open val kit: AbstractKit)
    : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    open val receiveScriptType = ScriptType.P2PKH
    open val changeScriptType = ScriptType.P2PKH
    abstract val satoshisInBitcoin: BigDecimal

    //
    // Adapter implementation
    //

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight: Int?
        get() = kit.lastBlockInfo?.height

    override val receiveAddress: String
        get() = kit.receiveAddress(receiveScriptType)

    protected val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val lastBlockHeightUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = lastBlockHeightUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = transactionRecordsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val debugInfo: String = ""

    override val balance: BigDecimal
        get() = BigDecimal.valueOf(kit.balance).divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)

    override var state: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            field = value
            adapterStateUpdatedSubject.onNext(Unit)
        }

    override fun start() {
        kit.start()
    }

    override fun stop() {
        kit.stop()
    }

    override fun refresh() {
        kit.refresh()
    }

    fun send(amount: BigDecimal, address: String, feeRate: Long): Single<Unit> {
        return Single.create { emitter ->
            try {
                kit.send(address, (amount * satoshisInBitcoin).toLong(), feeRate = feeRate.toInt(), changeScriptType = changeScriptType)
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(feeRate: Long, address: String?): BigDecimal {
        return BigDecimal.ZERO.max(balance.subtract(fee(balance, feeRate, address)))
    }

    fun fee(amount: BigDecimal, feeRate: Long, address: String?): BigDecimal {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            val fee = kit.fee(satoshiAmount, address, true, feeRate = feeRate.toInt(), changeScriptType = changeScriptType)
            BigDecimal.valueOf(fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: UnspentOutputSelectorError.InsufficientUnspentOutputs) {
            BigDecimal.valueOf(e.fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun validate(address: String) {
        kit.validateAddress(address)
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        return TransactionRecord(
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex,
                interTransactionIndex = 0,
                blockHeight = transaction.blockHeight?.toLong(),
                amount = transaction.amount.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                fee = transaction.fee?.toBigDecimal()?.divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                timestamp = transaction.timestamp,
                from = transaction.from.map { TransactionAddress(it.address, it.mine) },
                to = transaction.to.map { TransactionAddress(it.address, it.mine) }
        )
    }

    companion object {
        const val decimal = 8
    }

}

package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BitcoinBaseAdapter(open val kit: AbstractKit)
    : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    abstract val satoshisInBitcoin: BigDecimal

    //
    // Adapter implementation
    //

    override val confirmationsThreshold: Int = defaultConfirmationsThreshold
    override val lastBlockHeight: Int?
        get() = kit.lastBlockInfo?.height

    override val receiveAddress: String
        get() = kit.receiveAddress()

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
        get() = BigDecimal.valueOf(kit.balance.spendable).divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)

    override val balanceLocked: BigDecimal
        get() = BigDecimal.valueOf(kit.balance.unspendable).divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)

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

    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?): Single<Unit> {
        return Single.create { emitter ->
            try {
                kit.send(address, (amount * satoshisInBitcoin).toLong(), feeRate = feeRate.toInt(), pluginData = pluginData ?: mapOf())
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            BigDecimal.valueOf(kit.maximumSpendableValue(address, feeRate.toInt(), pluginData ?: mapOf())).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun minimumSendAmount(address: String?): BigDecimal {
        return BigDecimal.valueOf(kit.minimumSpendableValue(address).toLong()).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
    }

    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal? {
        return kit.maximumSpendLimit(pluginData)?.let { maximumSpendLimit ->
            BigDecimal.valueOf(maximumSpendLimit).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        }
    }

    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            val fee = kit.fee(satoshiAmount, address, true, feeRate = feeRate.toInt(), pluginData = pluginData ?: mapOf())
            BigDecimal.valueOf(fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        kit.validateAddress(address, pluginData ?: mapOf())
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
                from = transaction.from.map { TransactionAddress(it.address, it.mine, it.pluginData) },
                to = transaction.to.map { TransactionAddress(it.address, it.mine, it.pluginData) }
        )
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        const val defaultConfirmationsThreshold = 3
        const val decimal = 8
    }

}

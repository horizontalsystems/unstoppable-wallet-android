package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorError
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BitcoinBaseAdapter(override val wallet: Wallet, open val kit: AbstractKit, private val addressParser: AddressParser) : IAdapter {

    abstract val satoshisInBitcoin: BigDecimal
    abstract fun feeRate(feePriority: FeeRatePriority): Int

    //
    // Adapter implementation
    //

    override val feeCoinCode: String? = null
    override val decimal = 8

    override val confirmationsThreshold: Int = 6
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

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = addressParser.parse(address)
        return PaymentRequestAddress(paymentData.address, paymentData.amount?.toBigDecimal())
    }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority): Single<Unit> {
        return Single.create { emitter ->
            try {
                kit.send(address, (value * satoshisInBitcoin).toLong(), feeRate = feeRate(feePriority))
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun fee(value: BigDecimal, address: String?, feePriority: FeeRatePriority): BigDecimal {
        return try {
            val amount = (value * satoshisInBitcoin).toLong()
            val fee = kit.fee(amount, address, true, feeRate = feeRate(feePriority))
            BigDecimal.valueOf(fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: UnspentOutputSelectorError.InsufficientUnspentOutputs) {
            BigDecimal.valueOf(e.fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    override fun availableBalance(address: String?, feePriority: FeeRatePriority): BigDecimal {
        return BigDecimal.ZERO.max(balance.subtract(fee(balance, address, feePriority)))
    }

    override fun validate(amount: BigDecimal, address: String?, feePriority: FeeRatePriority): List<SendStateError> {
        val errors = mutableListOf<SendStateError>()
        if (amount > availableBalance(address, feePriority)) {
            errors.add(SendStateError.InsufficientAmount)
        }
        return errors
    }

    override fun validate(address: String) {
        kit.validateAddress(address)
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        return TransactionRecord(
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex,
                interTransactionIndex = 0,
                blockHeight = transaction.blockHeight?.toLong(),
                amount = transaction.amount.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                timestamp = transaction.timestamp,
                from = emptyList(),
                to = transaction.to.map { TransactionAddress(it.address, it.mine) }
        )
    }
}

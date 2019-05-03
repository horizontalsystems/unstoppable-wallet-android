package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BitcoinBaseAdapter(override val coin: Coin, val bitcoinKit: AbstractKit, private val addressParser: AddressParser) : IAdapter {

    abstract val satoshisInBitcoin: BigDecimal
    abstract fun feeRate(feePriority: FeeRatePriority): Int

    //
    // Adapter implementation
    //

    override val feeCoinCode: String? = null
    override val decimal = 8

    override val confirmationsThreshold: Int = 6
    override val lastBlockHeight = bitcoinKit.lastBlockInfo?.height
    override val receiveAddress: String = bitcoinKit.receiveAddress()

    override val balanceUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()
    override val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()
    override val debugInfo: String = ""

    override val balance: BigDecimal
        get() = BigDecimal.valueOf(bitcoinKit.balance).divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN)

    override var state: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            field = value
            adapterStateUpdatedSubject.onNext(Unit)
        }

    override fun start() {
        bitcoinKit.start()
    }

    override fun stop() {
        bitcoinKit.stop()
    }

    override fun refresh() {
        bitcoinKit.refresh()
    }

    override fun clear() {
        bitcoinKit.clear()
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        val paymentData = addressParser.parse(address)
        return PaymentRequestAddress(paymentData.address, paymentData.amount?.toBigDecimal())
    }

    override fun send(address: String, value: BigDecimal, feePriority: FeeRatePriority, completion: ((Throwable?) -> Unit)?) {
        try {
            bitcoinKit.send(address, (value * satoshisInBitcoin).toLong(), feeRate = feeRate(feePriority))
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: BigDecimal, address: String?, feePriority: FeeRatePriority): BigDecimal {
        return try {
            val amount = (value * satoshisInBitcoin).toLong()
            val fee = bitcoinKit.fee(amount, address, true, feeRate = feeRate(feePriority))
            BigDecimal.valueOf(fee).divide(satoshisInBitcoin, decimal, RoundingMode.CEILING)
        } catch (e: UnspentOutputSelector.Error.InsufficientUnspentOutputs) {
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
        bitcoinKit.validateAddress(address)
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return bitcoinKit.transactions(hashFrom, limit).map { tx -> tx.map { transactionRecord(it) } }
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        return TransactionRecord(
                transaction.transactionHash,
                transaction.blockHeight?.toLong() ?: 0,
                transaction.amount.toBigDecimal().divide(satoshisInBitcoin, decimal, RoundingMode.HALF_EVEN),
                transaction.timestamp,
                transaction.from.map { TransactionAddress(it.address, it.mine) },
                transaction.to.map { TransactionAddress(it.address, it.mine) }
        )
    }
}

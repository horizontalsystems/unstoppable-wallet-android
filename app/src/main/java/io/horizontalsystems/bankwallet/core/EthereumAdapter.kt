package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class EthereumAdapter(words: List<String>, network: NetworkType) : IAdapter, EthereumKit.Listener {

    private var ethereumKit = EthereumKit(words, network)
    private val weisInEther = Math.pow(10.0, 18.0)

    override val balance: Double get() = ethereumKit.balance
    override val balanceSubject: PublishSubject<Double> = PublishSubject.create()

    val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(1.0)

    override val state: AdapterState = AdapterState.Synced
    override val stateSubject: PublishSubject<AdapterState> = PublishSubject.create()

    override val confirmationsThreshold: Int = 12
    // todo replace with real ethereumKit.lastBlockHeight
    override val lastBlockHeight: Int? get() = 0
    override val lastBlockHeightSubject: PublishSubject<Int> = PublishSubject.create()

    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = ethereumKit.receiveAddress()

    override fun start() {
        ethereumKit.listener = this
        ethereumKit.start()
    }

    override fun refresh() {
        ethereumKit.refresh()
    }

    override fun clear() {
        ethereumKit.clear()
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        ethereumKit.send(address, value, completion)
    }

    override fun fee(value: Double, senderPay: Boolean): Double = ethereumKit.fee() / weisInEther

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    override fun balanceUpdated(ethereumKit: EthereumKit, balance: Double) {
        balanceSubject.onNext(balance / weisInEther)
    }

    override fun transactionsUpdated(ethereumKit: EthereumKit, inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val amountEther: Double = convertToValue(transaction.value) ?: 0.0

        val mineAddress = ethereumKit.receiveAddress().toLowerCase()

        val from = TransactionAddress()
        from.address = transaction.from
        from.mine = transaction.from.toLowerCase() == mineAddress

        val to = TransactionAddress()
        to.address = transaction.to
        to.mine = transaction.to.toLowerCase() == mineAddress

        val record = TransactionRecord()

        record.transactionHash = transaction.hash
        record.blockHeight = transaction.blockNumber
        record.amount = amountEther * if (from.mine) -1 else 1
        record.timestamp = transaction.timeStamp

        record.from = listOf(from)
        record.to = listOf(to)

        return record
    }

    private fun convertToValue(amount: String): Double? {
        val result = BigDecimal(amount)
        if (result != null) {
            return result.toDouble() / weisInEther
        }
        return null
    }

//    private fun calculateFee(gasUsed: String, gasPrice: String): Double {
//        val feeInWeis = BigDecimal(gasUsed).multiply(BigDecimal(gasPrice))
//        return feeInWeis.divide(weisInEther.toBigDecimal()).toDouble()
//    }
//
}

package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class EthereumAdapter(words: List<String>, network: NetworkType) : IAdapter, EthereumKit.Listener {

    private var ethereumKit = EthereumKit(words, network)
    private val weisInEther = Math.pow(10.0, 18.0)

    private val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(1.0)

    private val balanceSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(balance)
    private val stateSubject: BehaviorSubject<AdapterState> = BehaviorSubject.createDefault(AdapterState.Syncing(progressSubject))

    override val balance: Double
        get() = ethereumKit.balance

    override val balanceObservable: Flowable<Double> = balanceSubject.toFlowable(BackpressureStrategy.DROP)
    override val stateObservable: Flowable<AdapterState> = stateSubject.toFlowable(BackpressureStrategy.DROP)

    override val confirmationsThreshold: Int = 12
    override val lastBlockHeight: Int? get() = ethereumKit.lastBlockHeight
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

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return PaymentRequestAddress(address)
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        ethereumKit.send(address, value, completion)
    }

    override fun fee(value: Double, address: String?, senderPay: Boolean): Double {
        val fee = ethereumKit.fee() / weisInEther
        if (balance - value - fee < 0) {
            throw Error.InsufficientAmount(fee)
        }
        return fee
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    override fun balanceUpdated(balance: Double) {
        balanceSubject.onNext(balance / weisInEther)
    }

    override fun lastBlockHeightUpdated(height: Int) {
        lastBlockHeightSubject.onNext(height)
    }

    override fun onKitStateUpdate(state: EthereumKit.KitState) {
        when (state) {
            is EthereumKit.KitState.Synced -> {
                stateSubject.onNext(AdapterState.Synced)
            }
            is EthereumKit.KitState.NotSynced -> {
                stateSubject.onNext(AdapterState.NotSynced)
            }
            is EthereumKit.KitState.Syncing -> {
                progressSubject.onNext(state.progress)

                if (stateSubject.value !is AdapterState.Syncing) {
                    stateSubject.onNext(AdapterState.Syncing(progressSubject))
                }
            }
        }
    }

    override fun transactionsUpdated(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Flowable<List<TransactionRecord>> {
        return Flowable.just(listOf())
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val amountEther: Double = weisToEther(transaction.value) ?: 0.0

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

    private fun weisToEther(amount: String): Double? = try {
        BigDecimal(amount).toDouble() / weisInEther
    } catch (ex: Exception) {
        null
    }

//    private fun calculateFee(gasUsed: String, gasPrice: String): Double {
//        val feeInWeis = BigDecimal(gasUsed).multiply(BigDecimal(gasPrice))
//        return feeInWeis.divide(weisInEther.toBigDecimal()).toDouble()
//    }
//

    companion object {

        fun createEthereum(words: List<String>, testMode: Boolean): EthereumAdapter {
            val network = if (testMode) EthereumKit.NetworkType.Kovan else EthereumKit.NetworkType.MainNet
            return EthereumAdapter(words, network)
        }
    }

}

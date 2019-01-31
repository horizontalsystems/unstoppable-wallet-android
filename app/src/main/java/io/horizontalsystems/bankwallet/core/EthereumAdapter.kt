package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

class EthereumAdapter(words: List<String>, network: NetworkType, walletId: String) : IAdapter, EthereumKit.Listener {

    private var ethereumKit = EthereumKit(words, network, walletId)
    private val weisInEther = Math.pow(10.0, 18.0).toBigDecimal()

    private val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(1.0)

    private val balanceSubject: BehaviorSubject<BigDecimal> = BehaviorSubject.createDefault(balance)
    private val stateSubject: BehaviorSubject<AdapterState> = BehaviorSubject.createDefault(AdapterState.Syncing(progressSubject))

    override val balance: BigDecimal
        get() = ethereumKit.balance.toBigDecimal()

    override val balanceObservable: Flowable<BigDecimal> = balanceSubject.toFlowable(BackpressureStrategy.DROP)
    override val stateObservable: Flowable<AdapterState> = stateSubject.toFlowable(BackpressureStrategy.DROP)

    override val confirmationsThreshold: Int = 12
    override val lastBlockHeight: Int? get() = ethereumKit.lastBlockHeight
    override val lastBlockHeightUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val debugInfo: String = ""

    override val receiveAddress: String get() = ethereumKit.receiveAddress

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

    override fun send(address: String, value: BigDecimal, completion: ((Throwable?) -> (Unit))?) {
        ethereumKit.send(address, value.toDouble(), completion)
    }

    override fun fee(value: BigDecimal, address: String?, senderPay: Boolean): BigDecimal {
        val fee = ethereumKit.fee().toBigDecimal()
        if (senderPay && balance.minus(value).minus(fee) < BigDecimal.ZERO) {
            throw Error.InsufficientAmount(fee)
        }
        return fee
    }

    override fun validate(address: String) {
        ethereumKit.validateAddress(address)
    }

    override fun onBalanceUpdate(balance: Double) {
        balanceSubject.onNext(balance.toBigDecimal())
    }

    override fun onLastBlockHeightUpdate(height: Int) {
        lastBlockHeightUpdatedSignal.onNext(Unit)
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

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun getTransactionsObservable(hashFrom: String?, limit: Int): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(hashFrom, limit).map { it.map { transactionRecord(it) } }
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val amountEther: BigDecimal = weisToEther(transaction.value) ?: BigDecimal.ZERO
        val mineAddress = ethereumKit.receiveAddress.toLowerCase()

        val from = TransactionAddress()
        from.address = transaction.from
        from.mine = transaction.from.toLowerCase() == mineAddress

        val to = TransactionAddress()
        to.address = transaction.to
        to.mine = transaction.to.toLowerCase() == mineAddress

        return TransactionRecord(
                transaction.hash,
                transaction.blockNumber,
                if (from.mine) amountEther.unaryMinus() else amountEther,
                transaction.timeStamp,
                listOf(from),
                listOf(to)
        )
    }

    private fun weisToEther(amount: String): BigDecimal? = try {
        BigDecimal(amount).divide(weisInEther, 18, RoundingMode.HALF_EVEN)
    } catch (ex: Exception) {
        null
    }

    companion object {

        fun createEthereum(words: List<String>, testMode: Boolean, walletId: String): EthereumAdapter {
            val network = if (testMode)
                EthereumKit.NetworkType.Ropsten else
                EthereumKit.NetworkType.MainNet

            return EthereumAdapter(words, network, walletId)
        }
    }
}

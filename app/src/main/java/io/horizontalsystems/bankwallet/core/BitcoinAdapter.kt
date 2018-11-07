package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionStatus
import io.horizontalsystems.bankwallet.entities.coins.Coin
import io.horizontalsystems.bankwallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bankwallet.entities.coins.bitcoinCash.BitcoinCash
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class BitcoinAdapter(val words: List<String>, network: NetworkType) : IAdapter, BitcoinKit.Listener {

    private var bitcoinKit = BitcoinKit(words, network)
    private val transactionCompletionThreshold = 6
    private val satoshisInBitcoin = Math.pow(10.0, 8.0)

    override val coin: Coin = when (network) {
        NetworkType.RegTest -> Bitcoin("R")
        NetworkType.TestNet -> Bitcoin("T")
        NetworkType.MainNet -> Bitcoin()
        NetworkType.TestNetBitCash -> BitcoinCash("T")
        NetworkType.MainNetBitCash -> BitcoinCash()
    }
    override val id: String = "${words.joinToString(" ").hashCode()}-${coin.code}"

    override val balance: Double
        get() = bitcoinKit.balance / satoshisInBitcoin
    override val balanceSubject: PublishSubject<Double> = PublishSubject.create()

    override val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(0.0)

    override val latestBlockHeight: Int
        get() = bitcoinKit.lastBlockHeight
    override val latestBlockHeightSubject: PublishSubject<Any> = PublishSubject.create()

    override val transactionRecords: List<TransactionRecord>
        get() = bitcoinKit.transactions.map { transactionRecord(it) }
    override val transactionRecordsSubject: PublishSubject<Any> = PublishSubject.create()

    override val receiveAddress: String
        get() = bitcoinKit.receiveAddress()

    override fun debugInfo() {
    }

    override fun start() {
        bitcoinKit.listener = this
        bitcoinKit.start()
    }

    override fun refresh() {
    }

    override fun clear() {
        bitcoinKit.clear()
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        try {
            bitcoinKit.send(address, (value * satoshisInBitcoin).toInt())
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: Int, senderPay: Boolean): Double =
            bitcoinKit.fee(value = value, senderPay = senderPay) / satoshisInBitcoin

    override fun validate(address: String) = try {
        bitcoinKit.validateAddress(address)
        true
    } catch (ex: Exception) {
        false
    }

    private fun transactionRecord(transactionInfo: TransactionInfo): TransactionRecord {
        val confirmations = transactionInfo.blockHeight?.let {
            bitcoinKit.lastBlockHeight - it
        } ?: 0
        val txStatus = when {
            confirmations <= 0 -> TransactionStatus.Pending
            confirmations in 1 until transactionCompletionThreshold ->
                TransactionStatus.Processing(progress = (confirmations * 100 / transactionCompletionThreshold).toByte())
            else -> TransactionStatus.Completed
        }
        return TransactionRecord().apply {
            transactionHash = transactionInfo.transactionHash
            coinCode = coin.code
            from = transactionInfo.from.map { it.address }
            to = transactionInfo.to.map { it.address }
            amount = transactionInfo.amount / satoshisInBitcoin
            blockHeight = transactionInfo.blockHeight?.toLong()
            status = txStatus
            timestamp = transactionInfo.timestamp?.times(1000)
        }
    }

    //Bitcoin Kit listener methods

    override fun balanceUpdated(bitcoinKit: BitcoinKit, balance: Long) {
        balanceSubject.onNext(balance / satoshisInBitcoin)
    }

    override fun lastBlockInfoUpdated(bitcoinKit: BitcoinKit, lastBlockInfo: BlockInfo) {
        latestBlockHeightSubject.onNext(Any())
    }

    override fun progressUpdated(bitcoinKit: BitcoinKit, progress: Double) {
        progressSubject.onNext(progress)
    }

    override fun transactionsUpdated(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        transactionRecordsSubject.onNext(Any())
    }

}

package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.TransactionStatus
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.ethereum.Ethereum
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.network.NetworkType
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class EthereumAdapter(words: List<String>, network: NetworkType) : IAdapter, EthereumKit.Listener {

    private var ethereumKit = EthereumKit(words, network)
    private val transactionCompletionThreshold = 12
    private val weisInEther = Math.pow(10.0, 18.0)

    override val coin: Coin = Ethereum()
    override val id: String = "${words.joinToString(" ").hashCode()}-${coin.code}"

    override val balance: Double
        get() = ethereumKit.balance
    override val balanceSubject: PublishSubject<Double> = PublishSubject.create()

    override val progressSubject: BehaviorSubject<Double> = BehaviorSubject.createDefault(1.0)

    override var latestBlockHeight: Int = 0 //not used
    override val latestBlockHeightSubject: PublishSubject<Any> = PublishSubject.create()

    override val transactionRecords: List<TransactionRecord>
        get() = ethereumKit.transactions.map { transactionRecord(it) }
    override val transactionRecordsSubject: PublishSubject<Any> = PublishSubject.create()

    override val receiveAddress: String
        get() = ethereumKit.receiveAddress()

    override fun debugInfo() {
    }

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

    override fun fee(value: Int, senderPay: Boolean): Double = ethereumKit.fee()

    override fun validate(address: String) = try {
        ethereumKit.validateAddress(address)
        true
    } catch (ex: Exception) {
        false
    }

    private fun transactionRecord(ethereumTx: Transaction): TransactionRecord {
        val txStatus = when (ethereumTx.confirmations) {
            0 -> TransactionStatus.Pending
            in 1 until transactionCompletionThreshold ->
                TransactionStatus.Processing(progress = (ethereumTx.confirmations * 100 / transactionCompletionThreshold).toByte())
            else -> TransactionStatus.Completed
        }
        val incoming = ethereumKit.receiveAddress().toLowerCase() == ethereumTx.to.toLowerCase()

        return TransactionRecord().apply {
            transactionHash = ethereumTx.hash
            coinCode = coin.code
            from = listOf(ethereumTx.from)
            to = listOf(ethereumTx.to)
            amount = fromWeiToETH(ethereumTx.value) * (if (incoming) 1 else -1)
            fee = calculateFee(ethereumTx.gasUsed, ethereumTx.gasPrice)
            blockHeight = ethereumTx.blockNumber
            status = txStatus
            timestamp = ethereumTx.timeStamp * 1000 //convert to milliseconds
        }
    }

    private fun calculateFee(gasUsed: String, gasPrice: String): Double {
        val feeInWeis = BigDecimal(gasUsed).multiply(BigDecimal(gasPrice))
        return feeInWeis.divide(weisInEther.toBigDecimal()).toDouble()
    }

    private fun fromWeiToETH(weiAmount: String): Double =
            BigDecimal(weiAmount).divide(weisInEther.toBigDecimal()).toDouble()

    //Ethereum Kit listener methods

    override fun balanceUpdated(ethereumKit: EthereumKit, balance: Double) {
        balanceSubject.onNext(balance)
    }

    override fun transactionsUpdated(ethereumKit: EthereumKit, inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        transactionRecordsSubject.onNext(Any())
    }
}

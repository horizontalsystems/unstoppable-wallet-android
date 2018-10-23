package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.TransactionStatus
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.horizontalsystems.bitcoinkit.network.RegTest
import io.horizontalsystems.bitcoinkit.network.TestNet
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class BitcoinAdapter(words: List<String>, network: NetworkParameters) : IAdapter {

    private var walletKit = WalletKit(words, network)

    private val satoshisInBitcoin = Math.pow(10.0, 8.0)

    override val coin: Coin = when (network) {
        is RegTest -> Bitcoin("R")
        is TestNet -> Bitcoin("T")
        else -> Bitcoin()
    }
    override val id: String = "${words.joinToString(" ").hashCode()}-${coin.code}"

    override val balance: Double
        get() = walletKit.balance / satoshisInBitcoin
    override val balanceSubject: PublishSubject<Double> = PublishSubject.create()

    override val progressSubject: BehaviorSubject<Double> = walletKit.progressSubject

    override val latestBlockHeight: Int
        get() = walletKit.latestBlockHeight
    override val latestBlockHeightSubject: PublishSubject<Any> = PublishSubject.create()

    override val transactionRecords: List<TransactionRecord>
        get() = walletKit.transactionRecords
    override val transactionRecordsSubject: PublishSubject<Any> = PublishSubject.create()

    override val receiveAddress: String
        get() = walletKit.receiveAddress

    override fun debugInfo() {
        walletKit.showRealmInfo()
    }

    override fun start() {
        walletKit.start()
    }

    override fun refresh() {

    }

    override fun clear() {
        walletKit.clear()
    }

    override fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))?) {
        try {
            walletKit.send(address, (value * satoshisInBitcoin).toInt())
            completion?.invoke(null)
        } catch (ex: Exception) {
            completion?.invoke(ex)
        }
    }

    override fun fee(value: Int, senderPay: Boolean): Double {
        return walletKit.fee(value, senderPay).div(satoshisInBitcoin)
    }

    override fun validate(address: String): Boolean {
        return true
    }

}

//Stub class from WalletKit
class WalletKit(words: List<String>, network: NetworkParameters) {
    val latestBlockHeight = 129
    val transactionRecords: List<TransactionRecord> = demoTransactions()
    val receiveAddress = "1AYHMDV1XR8HWaReC3Rr4Qv79vJiSR8RCU"
    val progressSubject: BehaviorSubject<Double> = BehaviorSubject.create()
    val balance: Long
        get() = 24 * Math.pow(10.0, 8.0).toLong()

    fun showRealmInfo() {

    }

    fun start() {

    }

    fun clear() {

    }

    fun send(address: String, value: Int) {

    }

    fun fee(value: Int, senderPay: Boolean): Int {
        return 0
    }


    //demo transactions
    fun demoTransactions(): List<TransactionRecord> {
        val transactions: MutableList<TransactionRecord> = mutableListOf()
        val tr = TransactionRecord().apply {
            transactionHash = "1ayhmdv1xr8dhdnkbkjbdeef8dfa8kmnbbydf9pq"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.025
            fee = 0.0093
            blockHeight = 130
            status = TransactionStatus.Processing(33)
            timestamp = 1539206581000
        }
        transactions.add(tr)

        val tr1 = TransactionRecord().apply {
            transactionHash = "1byhmdv1xr8dhdnkbkjbdeef8dfa8kmnbbydf9pq"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.03
            fee = 0.0093
            blockHeight = 122
            status = TransactionStatus.Completed
            timestamp = 1538893392000
        }
        transactions.add(tr1)

        val tr2 = TransactionRecord().apply {
            transactionHash = "1cyhmdv1xr8dhdnkbkjbdeef8dfa8kmnbbydf9pq"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.032
            fee = 0.0093
            blockHeight = 105
            status = TransactionStatus.Completed
            timestamp = 1538461392000
        }
        transactions.add(tr2)

        val tr3 = TransactionRecord().apply {
            transactionHash = "1dyhmdv1xr8dhdnkbkjbdeef8dfa8kmnbbydf9pq"
            coinCode = "BTC"
            from = listOf("mxNEBQf2xQeLknPZW65rMbKxEban6udxFc")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = -0.23
            fee = 0.00012
            blockHeight = 128
            status = TransactionStatus.Pending
            timestamp = 1538288592000
        }
        transactions.add(tr3)

        val tr4 = TransactionRecord().apply {
            transactionHash = "1eyhmdv1xr8dhdnkbkjbdeef8dfa8kmnbbydf9pq"
            coinCode = "BTC"
            from = listOf("wef23mxNEBQf2xQeLknPZW65rMbKxEban6udxFc")
            to = listOf("ew13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.183
            fee = 0.00092
            blockHeight = 103
            status = TransactionStatus.Completed
            timestamp = 1538131030582
        }
        transactions.add(tr4)

        return transactions
    }
}

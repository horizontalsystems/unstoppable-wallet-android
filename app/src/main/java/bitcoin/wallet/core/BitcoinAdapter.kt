package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecordNew
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.RegTest
import bitcoin.wallet.kit.network.TestNet
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class BitcoinAdapter(words: List<String>, network: NetworkParameters) : IAdapter {

    private var walletKit: WalletKit
    var wordsHash: String = words.joinToString(" ")

    init {
        walletKit = WalletKit(words, network)
        println("BitcoinAdapter started with words $words")

        //for test purpose
//        Handler().postDelayed({
//            progressSubject.onNext(0.0)
//
//        }, (1 * 1000).toLong())
//
//        Handler().postDelayed({
//            updateBalance(2091183337)
//            progressSubject.onNext(1.0)
//
//        }, (10 * 1000).toLong())
    }

    override var coin: Coin = when (network) {
        is RegTest -> Bitcoin("R")
        is TestNet -> Bitcoin("T")
        else -> Bitcoin()
    }

    override var id: String = "${wordsHash.hashCode()}-${coin.code}"
    override var balanceSubject: PublishSubject<Double> = PublishSubject.create()
    override var balance: Double = 24.0//0.0
        set(value) {
            field = value
            balanceSubject.onNext(field)
        }

    //    private var transactionsNotificationToken: NotificationToken?
//    private var unspentOutputsNotificationToken: NotificationToken?
//    override var latestBlockHeightSubject: PublishSubject<Void> = PublishSubject.create()
    override var transactionRecordsSubject: PublishSubject<Void> = PublishSubject.create()


    override var latestBlockHeight: Int = walletKit.latestBlockHeight
    override var transactionRecords: List<TransactionRecordNew> = walletKit.transactionRecords

    override fun showInfo() {
        walletKit.showRealmInfo()
    }

    override fun start() {
        walletKit.start()
    }

    override fun clear() {
        walletKit.clear()
    }

    override fun send(address: String, value: Int) {
        walletKit.send(address, value)
    }

    override fun fee(value: Int, senderPay: Boolean): Int {
        return walletKit.fee(value, senderPay)
    }

    override fun validate(address: String): Boolean {
        return true
    }

    override var progressSubject: BehaviorSubject<Double> = walletKit.progressSubject

    override var receiveAddress: String = walletKit.receiveAddress

    private fun updateBalance() {
        var satoshiBalance = 0

//        for output in walletKit.unspentOutputsRealmResults {
//            satoshiBalance += output.value
//        }

        balance = satoshiBalance / 100000000.0
    }
}

//Stub class from WalletKit
class WalletKit(words: List<String>, network: NetworkParameters) {
    val latestBlockHeight = 129
    val transactionRecords: List<TransactionRecordNew> = demoTransactions()//listOf()
    val receiveAddress = "addressSomeTesting32String"
    val progressSubject: BehaviorSubject<Double> = BehaviorSubject.create()

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
    fun demoTransactions(): List<TransactionRecordNew> {
        val transactions: MutableList<TransactionRecordNew> = mutableListOf()
        val tr = TransactionRecordNew().apply {
            transactionHash = "1AYHMDV1XR8DHDNKBKJBDEEF8DFA8KMNBBYDF9PQ"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.025
            fee = 0.0093
            blockHeight = 130
            timestamp = 1536652171123
        }
        transactions.add(tr)

        val tr1 = TransactionRecordNew().apply {
            transactionHash = "1ANKNJ1XR8DHDNKBKJBDEEF8DFA8KMNBBYDF9PQ"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.03
            fee = 0.0093
            blockHeight = 128
            timestamp = 1533152151123
        }
        transactions.add(tr1)

        val tr2 = TransactionRecordNew().apply {
            transactionHash = "1AYHMDV1XR8DHDNKBKJBDEEF8DFA8KMNBBYDF9PQ"
            coinCode = "BTC"
            from = listOf("1A7o3DtwdLQWy9dMq5oV9CHW1PC8jrfFPi")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.032
            fee = 0.0093
            blockHeight = 125
            timestamp = 1533052151123
        }
        transactions.add(tr2)

        val tr3 = TransactionRecordNew().apply {
            transactionHash = "1AYHMDV1XR8DHDNKBKJBDEEF8DFA8KMNBBYDF9PQ"
            coinCode = "BTC"
            from = listOf("mxNEBQf2xQeLknPZW65rMbKxEban6udxFc")
            to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = -0.23
            fee = 0.00012
            blockHeight = 122
            timestamp = 1536152151123
        }
        transactions.add(tr3)

        val tr4 = TransactionRecordNew().apply {
            transactionHash = "1AYHMDV1XR8DHDNKBKJBDEEF8DFA8KMNBBYDF9PQ"
            coinCode = "BTC"
            from = listOf("wef23mxNEBQf2xQeLknPZW65rMbKxEban6udxFc")
            to = listOf("ew13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
            amount = 0.183
            fee = 0.00092
            blockHeight = 103
            timestamp = 1536052151123
        }
        transactions.add(tr4)

        return transactions
    }
}

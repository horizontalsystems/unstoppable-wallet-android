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
    val latestBlockHeight = 0
    val transactionRecords: List<TransactionRecordNew> = listOf()
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
}

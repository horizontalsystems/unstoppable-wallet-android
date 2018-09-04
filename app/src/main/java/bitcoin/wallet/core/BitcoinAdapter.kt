package bitcoin.wallet.core

import android.util.Log
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.RegTest
import bitcoin.wallet.kit.network.TestNet
import io.realm.Realm
import io.realm.RealmConfiguration

class BitcoinAdapter(words: List<String>, network: NetworkParameters) : IAdapter {

    private var walletKit: WalletKit
    var wordsHash: String = words.joinToString(" ")

    init {
        //code related to Realm
        val realmFileName = "${network.paymentProtocolId}-${Integer.toHexString(wordsHash.hashCode())}.dat"
//        val walletFile = File(filesDir, realmFileName)
        val configuration = Realm.getDefaultConfiguration()//RealmConfiguration()
        walletKit = WalletKit(words, configuration!!, network)
        println("BitcoinAdapter started with words $words")
        Log.e("BitcoinAdapter", "BitcoinAdapter started with words $words")
    }

    override var coin: Coin = when (network) {
        is RegTest -> Bitcoin("R")
        is TestNet -> Bitcoin("T")
        else -> Bitcoin()
    }

    override var id: String = "${wordsHash.hashCode()}-${coin.code}"
    override var balance: Long = 2091183337

//    private var transactionsNotificationToken: NotificationToken?
//    private var unspentOutputsNotificationToken: NotificationToken?
//    override var balanceSubject: PublishSubject<Double> = PublishSubject.create()
//    override var latestBlockHeightSubject: PublishSubject<Void> = PublishSubject.create()
//    override var transactionRecordsSubject: PublishSubject<Void> = PublishSubject.create()


    override var latestBlockHeight: Int = walletKit.latestBlockHeight
    override var transactionRecords: List<TransactionRecord> = walletKit.transactionRecords

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

    override var receiveAddress: String = walletKit.receiveAddress

}

//Stub class from WalletKit
class WalletKit(words: List<String>, realmConfiguration: RealmConfiguration, network: NetworkParameters) {
    val latestBlockHeight = 0
    val transactionRecords: List<TransactionRecord> = listOf() //todo get transactions from database
    val receiveAddress = ""

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

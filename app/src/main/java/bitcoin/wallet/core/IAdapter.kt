package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.Coin


interface IAdapter {
    var id: String
    var coin: Coin
    var balance: Long

//    var balanceSubject: PublishSubject<Double>
//    var latestBlockHeightSubject: PublishSubject<Void>
//    var transactionRecordsSubject: PublishSubject<Void>

    //    var progressSubject: BehaviorSubject<Double> { get }

    var latestBlockHeight: Int

    var transactionRecords: List<TransactionRecord>

    fun showInfo()

    fun start()
    fun clear()

    fun send(address: String, value: Int)
    fun fee(value: Int, senderPay: Boolean): Int
    fun validate(address: String): Boolean

    var receiveAddress: String
}

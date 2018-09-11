package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecordNew
import bitcoin.wallet.entities.coins.Coin
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject


interface IAdapter {
    var id: String
    var coin: Coin
    var balance: Double

    var balanceSubject: PublishSubject<Double>
    var progressSubject: BehaviorSubject<Double>
//    var latestBlockHeightSubject: PublishSubject<Void>
    var transactionRecordsSubject: PublishSubject<Void>


    var latestBlockHeight: Int

    var transactionRecords: List<TransactionRecordNew>

    fun showInfo()

    fun start()
    fun clear()

    fun send(address: String, value: Int)
    fun fee(value: Int, senderPay: Boolean): Int
    fun validate(address: String): Boolean

    var receiveAddress: String
}

package bitcoin.wallet.core

import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.Coin
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject


interface IAdapter {
    val id: String
    val coin: Coin

    val balance: Double
    val balanceSubject: PublishSubject<Double>

    val progressSubject: BehaviorSubject<Double>

    val latestBlockHeight: Int
    val latestBlockHeightSubject: PublishSubject<Any>

    val transactionRecords: List<TransactionRecord>
    val transactionRecordsSubject: PublishSubject<Any>

    val receiveAddress: String

    fun debugInfo()

    fun start()
    fun refresh()
    fun clear()

    fun send(address: String, value: Double, completion: ((Throwable?) -> (Unit))? = null)
    fun fee(value: Int, senderPay: Boolean): Double
    fun validate(address: String): Boolean

}

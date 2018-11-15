package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import java.util.*

class StubStorage: ITransactionRecordStorage, IRateStorage {

    override fun record(hash: String): TransactionRecord? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val nonFilledRecords: List<TransactionRecord>
        get() = transactionRecords

    override fun set(rate: Double, transactionHash: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearRates() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(records: List<TransactionRecord>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearRecords() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rate(coin: String, currencyCode: String): Rate? {
        return when(coin) {
            "ETHt" -> Rate("ETHt", "USD", 200.0, 1542080725000)
            "BTCt" -> Rate("BTCt", "USD", 10_000.0, 1542080725000)
            else -> Rate("BTC", "USD", 6300.0, 1542080725000)
        }
    }

    override fun save(value: Double, coin: String, currencyCode: String) {
        Log.e("RateDataStorage", "save() called with value $value for currency $currencyCode")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        Log.e("RateDataStorage", "clear() called")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val transactionRecords = listOf(
            TransactionRecord().apply {
                transactionHash = "transactionHash"
                blockHeight = 135
                coin = "BTCt"
                amount = 1.0
                timestamp = Date().time / 1000
                rate = 6500.0

                from = listOf(TransactionAddress().apply {
                    address = "fromaddress"
                    mine = false
                })

                to = listOf(TransactionAddress().apply {
                    address = "toaddress"
                    mine = true
                })
            },
            TransactionRecord().apply {
                transactionHash = "transactionHash"
                blockHeight = 442
                coin = "ETHt"
                amount = 2.10
                timestamp = Date(System.currentTimeMillis() - ((24 * 60 - 25) * 60 * 1000)).time / 1000
                rate = 200.0

                from = listOf(TransactionAddress().apply {
                    address = "fromaddress"
                    mine = false
                })

                to = listOf(TransactionAddress().apply {
                    address = "toaddress"
                    mine = true
                })
            }
    )
}
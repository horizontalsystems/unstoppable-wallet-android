package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import java.util.*

class TransactionRecordDataSource : TransactionsModule.ITransactionRecordDataSource {

    private var coin: Coin? = null

    override var delegate: TransactionsModule.ITransactionRecordDataSourceDelegate? = null

    override val count: Int
        get() = xxx(coin).size

    override fun recordForIndex(index: Int): TransactionRecord {
        return xxx(coin)[index]
    }

    override fun setCoin(coin: Coin?) {
        this.coin = coin
        delegate?.onUpdateResults()
    }
}

fun xxx(coin: Coin?) = when (coin) {
    null -> transactionRecords
    else -> transactionRecords.filter { it.coin == coin }
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
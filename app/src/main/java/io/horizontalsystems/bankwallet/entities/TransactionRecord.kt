package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

data class TransactionRecord(var transactionHash: String) {
    var blockHeight: Long = 0L
    var amount: BigDecimal = BigDecimal.ZERO
    var timestamp: Long = 0L
    var from: List<TransactionAddress> = listOf()
    var to: List<TransactionAddress> = listOf()

    constructor(transactionHash: String, blockHeight: Long, amount: BigDecimal, timestamp: Long, from: List<TransactionAddress>, to: List<TransactionAddress>) : this(transactionHash) {
        this.blockHeight = blockHeight
        this.amount = amount
        this.timestamp = timestamp
        this.from = from
        this.to = to
    }

}

data class TransactionItem(val coinCode: CoinCode, val record: TransactionRecord)

class TransactionAddress {
    var address = ""
    var mine = false
}

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

    override fun equals(other: Any?): Boolean {
        if (other is TransactionRecord) {
            return transactionHash == other.transactionHash
                    && blockHeight == other.blockHeight
                    && amount == other.amount
                    && timestamp == other.timestamp
                    && from == other.from
                    && to == other.to
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = transactionHash.hashCode()
        result = 31 * result + blockHeight.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }
}

data class TransactionItem(val coinCode: CoinCode, val record: TransactionRecord)

class TransactionAddress(val address: String, val mine: Boolean)

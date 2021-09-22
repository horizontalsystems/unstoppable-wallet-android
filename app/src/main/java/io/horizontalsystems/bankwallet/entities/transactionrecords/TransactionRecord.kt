package io.horizontalsystems.bankwallet.entities.transactionrecords

import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus

abstract class TransactionRecord(
    val uid: String,
    val transactionHash: String,
    val transactionIndex: Int,
    val blockHeight: Int?,
    val confirmationsThreshold: Int?,
    val timestamp: Long,
    val failed: Boolean = false,
    val source: TransactionSource
) : Comparable<TransactionRecord> {

    val blockchainTitle: String = source.blockchain.getTitle()

    open val mainValue: TransactionValue? = null

    open fun changedBy(oldBlockInfo: LastBlockInfo?, newBlockInfo: LastBlockInfo?): Boolean =
        status(oldBlockInfo?.height) != status(newBlockInfo?.height)

    override fun compareTo(other: TransactionRecord): Int {
        return when {
            timestamp != other.timestamp -> timestamp.compareTo(other.timestamp)
            transactionIndex != other.transactionIndex -> transactionIndex.compareTo(other.transactionIndex)
            else -> uid.compareTo(uid)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionRecord) {
            return uid == other.uid
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    fun status(lastBlockHeight: Int?): TransactionStatus {

        if (failed) {
            return TransactionStatus.Failed
        } else if (blockHeight != null && lastBlockHeight != null) {
            val threshold = confirmationsThreshold ?: 1
            val confirmations = lastBlockHeight - blockHeight.toInt() + 1

            if (confirmations >= threshold) {
                return TransactionStatus.Completed
            } else {
                return TransactionStatus.Processing(confirmations.toDouble() / threshold.toDouble())
            }
        }

        return TransactionStatus.Pending
    }
}

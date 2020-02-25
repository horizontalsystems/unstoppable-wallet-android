package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import java.math.BigDecimal

data class TransactionRecord(
        val uid: String,
        val transactionHash: String,
        val transactionIndex: Int,
        val interTransactionIndex: Int,
        val blockHeight: Long?,
        val amount: BigDecimal,
        val fee: BigDecimal? = null,
        val timestamp: Long,
        val from: String?,
        val to: String?,
        val type: TransactionType,
        val lockInfo: TransactionLockInfo? = null,
        val failed: Boolean = false,
        val conflictingTxHash: String? = null)
    : Comparable<TransactionRecord> {

    override fun compareTo(other: TransactionRecord): Int {
        return when {
            timestamp != other.timestamp -> timestamp.compareTo(other.timestamp)
            transactionIndex != other.transactionIndex -> transactionIndex.compareTo(other.transactionIndex)
            else -> interTransactionIndex.compareTo(other.interTransactionIndex)
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
}

data class TransactionItem(val wallet: Wallet, val record: TransactionRecord) : Comparable<TransactionItem> {
    override fun compareTo(other: TransactionItem): Int {
        return record.compareTo(other.record)
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionItem) {
            return record == other.record
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return record.hashCode()
    }
}

enum class TransactionType {
    Incoming, Outgoing, SentToSelf
}


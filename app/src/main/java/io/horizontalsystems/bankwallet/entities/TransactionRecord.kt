package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionLockState
import java.math.BigDecimal

data class TransactionRecord(
        val uid: String,
        val transactionHash: String,
        val transactionIndex: Int,
        val interTransactionIndex: Int,
        val blockHeight: Long?,
        val confirmationsThreshold: Int,
        val amount: BigDecimal,
        val fee: BigDecimal? = null,
        val timestamp: Long,
        val from: String?,
        val memo: String?,
        val to: String?,
        val type: TransactionType,
        val lockInfo: TransactionLockInfo? = null,
        val failed: Boolean = false,
        val conflictingTxHash: String? = null,
        val showRawTransaction: Boolean = false)
    : Comparable<TransactionRecord> {

    override fun compareTo(other: TransactionRecord): Int {
        return when {
            timestamp != other.timestamp -> timestamp.compareTo(other.timestamp)
            transactionIndex != other.transactionIndex -> transactionIndex.compareTo(other.transactionIndex)
            interTransactionIndex != other.interTransactionIndex -> interTransactionIndex.compareTo(other.interTransactionIndex)
            type != other.type -> type.compareTo(other.type)
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
        var status: TransactionStatus = TransactionStatus.Pending

        if (failed) {
            status = TransactionStatus.Failed
        } else if (blockHeight != null && lastBlockHeight != null) {
            val confirmations = lastBlockHeight - blockHeight.toInt() + 1
            if (confirmations >= 0) {
                status = when {
                    confirmations >= confirmationsThreshold -> TransactionStatus.Completed
                    else -> TransactionStatus.Processing(confirmations.toDouble() / confirmationsThreshold.toDouble())
                }
            }
        }

        return status
    }

    fun lockState(lastBlockTimestamp: Long?): TransactionLockState? {
        if (lockInfo == null) return null

        val locked = lastBlockTimestamp?.let {
            lastBlockTimestamp < (lockInfo.lockedUntil.time / 1000)
        } ?: true

        return TransactionLockState(locked, lockInfo.lockedUntil)
    }
}

enum class TransactionType {
     Outgoing, SentToSelf, Incoming, Approve
}


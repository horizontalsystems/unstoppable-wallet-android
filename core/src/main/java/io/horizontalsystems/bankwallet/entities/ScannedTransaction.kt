package io.horizontalsystems.bankwallet.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.horizontalsystems.bankwallet.core.managers.PoisoningScorer
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(
    indices = [Index(value = ["address", "spamScore"])]
)
data class ScannedTransaction(
    @PrimaryKey val transactionHash: ByteArray,
    val spamScore: Int,
    val blockchainType: BlockchainType,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val address: String?
) {
    val isSpam: Boolean
        get() = spamScore >= PoisoningScorer.SPAM_THRESHOLD

    val isSuspicious: Boolean
        get() = spamScore in PoisoningScorer.SUSPICIOUS_THRESHOLD until PoisoningScorer.SPAM_THRESHOLD

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScannedTransaction
        return transactionHash.contentEquals(other.transactionHash)
    }

    override fun hashCode(): Int {
        return transactionHash.contentHashCode()
    }
}
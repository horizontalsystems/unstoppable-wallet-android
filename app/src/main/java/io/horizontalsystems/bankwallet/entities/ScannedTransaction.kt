package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity
data class ScannedTransaction(
    @PrimaryKey val transactionHash: ByteArray,
    val isSpam: Boolean,
    val blockchainType: BlockchainType,
    val address: String?
) {
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
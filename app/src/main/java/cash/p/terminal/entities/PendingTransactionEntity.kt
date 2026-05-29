package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "PendingTransaction")
data class PendingTransactionEntity(
    @PrimaryKey
    val id: String,

    // Wallet & token identification
    val walletId: String,
    val coinUid: String,
    val blockchainTypeUid: String,
    val tokenTypeId: String,
    val meta: String?,

    // Transaction data for matching
    val amountAtomic: String,
    val feeAtomic: String?,
    val sdkBalanceAtCreationAtomic: String,
    val fromAddress: String,
    val toAddress: String,

    // Metadata
    val txHash: String? = null,
    val nonce: Long? = null,
    val memo: String? = null,

    // Lifecycle
    val createdAt: Long,
    val expiresAt: Long,
    val balanceConfirmedAt: Long? = null
)

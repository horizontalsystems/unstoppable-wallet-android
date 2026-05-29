package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OcpPaymentRecord(
    @PrimaryKey val txHash: String,
    val paymentId: String,
    val quoteId: String,
    val proofUrl: String,
    val method: String,
    val merchant: String?,
    val expirationIso: String?,
    val createdAt: Long,
    val proofSubmittedAt: Long?,
    val proofFailedAt: Long? = null,
)

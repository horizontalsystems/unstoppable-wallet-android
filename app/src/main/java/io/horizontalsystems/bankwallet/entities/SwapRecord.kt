package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SwapRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: String,
    val timestamp: Long,
    val providerId: String,
    val providerName: String,

    val tokenInUid: String,
    val tokenInCoinCode: String,
    val tokenInCoinUid: String,
    val tokenInBadge: String?,
    val tokenInBlockchainTypeUid: String,

    val tokenOutUid: String,
    val tokenOutCoinCode: String,
    val tokenOutCoinUid: String,
    val tokenOutBadge: String?,
    val tokenOutBlockchainTypeUid: String,

    val amountIn: String,
    val amountOut: String?,
    val amountOutMin: String?,

    val recipientAddress: String?,
    val sourceAddress: String?,
    val transactionHash: String?,
    val outboundTransactionHash: String? = null,

    val providerSwapId: String?,
    val fromAsset: String?,
    val toAsset: String?,
    val depositAddress: String?,
    val status: String,
)
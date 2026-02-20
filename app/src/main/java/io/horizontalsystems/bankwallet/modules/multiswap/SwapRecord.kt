package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SwapStatus {
    Depositing,
    Swapping,
    Sending,
    Completed,
    Refunded,
    Failed,
}

@Entity
data class SwapRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
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

    val slippage: String?,
    val networkFeeCoinCode: String?,
    val networkFeeAmount: String?,

    val providerSwapId: String?,
    val fromAsset: String?,
    val toAsset: String?,
    val depositAddress: String?,
    val status: String,
)

package io.horizontalsystems.walletkit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import java.security.MessageDigest

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
    val customRecipientAddress: Boolean = false,
    val sourceAddress: String?,
    val transactionHash: String?,
    val outboundTransactionHash: String? = null,

    val providerSwapId: String?,
    val fromAsset: String?,
    val toAsset: String?,
    val depositAddress: String?,
    val status: String,
    // SwapOperation.name — which user-facing operation produced this record (plain swap,
    // private send, CrossPay). Same rail can serve several; history renders each differently.
    val operation: String = SwapOperation.Swap.name,
    val pauseReason: String? = null,
    // opaque handle linking the record to an external settlement mechanism
    // (e.g. a smart-account userOp hash) until the real transactionHash is known
    val trackingHandle: String? = null,
    // estimated swap duration in seconds, as quoted at swap time
    val estimatedTime: Long? = null,
    // SplitSwap only: the outputs as JSON (SwapRecordOutput list), each with its own status,
    // amount and payout hash, since the outputs of one deposit finish independently.
    val splitOutputs: String? = null,
)

/**
 * Every column of the record as stored, one `name: value` line per column in declaration
 * order, with a null column written as an empty value. This is what "Copy Raw Data" on Swap
 * Info puts on the clipboard, so a user can hand over the exact row when a swap goes wrong
 * on a release build, where the database cannot be read directly. Each column is listed by
 * hand; [SwapRecordRawDataTest] fails when a column is added here without a line below.
 *
 * The last line is an integrity check: the first 16 hex digits of the SHA-256 of the text
 * above it (without a trailing newline). It catches a paste that lost or changed a line on
 * the way to us. It is not a guard against a deliberate edit: anyone can recompute it.
 */
fun SwapRecord.toRawData(): String {
    val body = listOf(
        "id" to id,
        "accountId" to accountId,
        "timestamp" to timestamp,
        "providerId" to providerId,
        "providerName" to providerName,
        "tokenInUid" to tokenInUid,
        "tokenInCoinCode" to tokenInCoinCode,
        "tokenInCoinUid" to tokenInCoinUid,
        "tokenInBadge" to tokenInBadge,
        "tokenInBlockchainTypeUid" to tokenInBlockchainTypeUid,
        "tokenOutUid" to tokenOutUid,
        "tokenOutCoinCode" to tokenOutCoinCode,
        "tokenOutCoinUid" to tokenOutCoinUid,
        "tokenOutBadge" to tokenOutBadge,
        "tokenOutBlockchainTypeUid" to tokenOutBlockchainTypeUid,
        "amountIn" to amountIn,
        "amountOut" to amountOut,
        "amountOutMin" to amountOutMin,
        "recipientAddress" to recipientAddress,
        "customRecipientAddress" to customRecipientAddress,
        "sourceAddress" to sourceAddress,
        "transactionHash" to transactionHash,
        "outboundTransactionHash" to outboundTransactionHash,
        "providerSwapId" to providerSwapId,
        "fromAsset" to fromAsset,
        "toAsset" to toAsset,
        "depositAddress" to depositAddress,
        "status" to status,
        "operation" to operation,
        "pauseReason" to pauseReason,
        "trackingHandle" to trackingHandle,
        "estimatedTime" to estimatedTime,
        "splitOutputs" to splitOutputs,
    ).joinToString("\n") { (name, value) -> "$name: ${value ?: ""}" }

    return body + "\nchecksum: " + rawDataChecksum(body)
}

fun rawDataChecksum(body: String): String = MessageDigest.getInstance("SHA-256")
    .digest(body.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
    .take(16)

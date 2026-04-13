package cash.p.terminal.core.managers

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import java.math.BigDecimal
import kotlin.math.abs

data class MatchScore(
    val isMatch: Boolean,
    val confidence: Double
)

class PendingTransactionMatcher {

    private fun calculateMatchScoreInternal(
        txHash: String,
        timestampPending: Long,
        blockchainTypeUid: String,
        amountAtomic: String,
        toAddress: String,
        real: TransactionRecord
    ): MatchScore {
        // Priority 1: Hash Match (confidence = 1.0)
        if (txHash.isNotEmpty() && txHash == real.transactionHash) {
            return MatchScore(isMatch = true, confidence = 1.0)
        }

        // Priority 2: Fuzzy Match (confidence = 0.9)
        val blockchainMatches = blockchainTypeUid == real.blockchainType.uid
        val amountMatches = compareAmounts(amountAtomic, real)
        val realTo = real.to?.firstOrNull()

        if (blockchainMatches && amountMatches) {
            val addressMatches = realTo != null && toAddress.isNotEmpty() &&
                    toAddress.equals(realTo, ignoreCase = true)
            if (addressMatches) {
                return MatchScore(isMatch = true, confidence = 0.9)
            } else if (compareTimestamps(timestampPending, real.timestamp)) {
                return MatchScore(isMatch = true, confidence = 0.8)
            }
        }

        return MatchScore(isMatch = false, confidence = 0.0)
    }

    fun calculateMatchScore(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore = calculateMatchScoreInternal(
        txHash = pending.transactionHash,
        timestampPending = pending.timestamp,
        blockchainTypeUid = pending.blockchainType.uid,
        amountAtomic = pending.amount.movePointRight(pending.token.decimals).toBigInteger()
            .toString(),
        toAddress = pending.to?.firstOrNull() ?: "",
        real = real
    )

    private fun compareAmounts(
        pendingAmountAtomic: String,
        real: TransactionRecord
    ): Boolean {
        return try {
            val pendingAmount = BigDecimal(pendingAmountAtomic)
                .movePointLeft(getRealDecimal(real))
                .abs()

            val realAmount = getRealAmount(real)?.abs() ?: return false

            // Allow 0.1% difference for floating point errors
            val difference = (pendingAmount - realAmount).abs()
            val threshold = pendingAmount.multiply(BigDecimal("0.001"))

            difference <= threshold
        } catch (e: Exception) {
            false
        }
    }

    private fun getRealDecimal(real: TransactionRecord): Int {
        val value = if(real is TonTransactionRecord) {
            real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Swap }
                ?.let { action ->
                    (action.type as? TonTransactionRecord.Action.Type.Swap)?.valueIn?.decimals
                }
                ?: real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Send }
                    ?.let { action ->
                        (action.type as? TonTransactionRecord.Action.Type.Send)?.value?.decimals
                    }
        } else {
            null
        }
        return value ?: real.token.decimals
    }

    private fun compareTimestamps(
        timestampPending: Long,
        timestampReal: Long
    ): Boolean {
        val differenceSeconds = abs(timestampPending - timestampReal)
        return differenceSeconds <= 10 // 10 seconds tolerance
    }

    private fun getRealAmount(
        real: TransactionRecord
    ): BigDecimal? {
        val tonValue = if (real is TonTransactionRecord) {
            real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Swap }
                ?.let { action ->
                    (action.type as? TonTransactionRecord.Action.Type.Swap)?.valueIn?.decimalValue
                }
                ?: real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Send }
                    ?.let { action ->
                        (action.type as? TonTransactionRecord.Action.Type.Send)?.value?.decimalValue
                    }
        } else {
            null
        }
        if (tonValue != null) {
            return tonValue
        }

        return when (val mainValue = real.mainValue) {
            is TransactionValue.CoinValue -> mainValue.value
            else -> null
        }
    }
}

package cash.p.terminal.core.managers

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.wallet.Token
import java.math.BigDecimal
import kotlin.math.abs

data class MatchScore(
    val isMatch: Boolean,
    val confidence: Double
)

class PendingTransactionMatcher {

    fun matchScoreForRealRecord(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore {
        if (pending.transactionHash.isNotEmpty() && pending.transactionHash == real.transactionHash) {
            return MatchScore(isMatch = true, confidence = 1.0)
        }

        if (real is PendingTransactionRecord || !real.isOutgoingForPendingMatch()) {
            return MatchScore(isMatch = false, confidence = 0.0)
        }

        if (real.assetTokenForPendingMatch() != pending.token) {
            return MatchScore(isMatch = false, confidence = 0.0)
        }

        return calculateFuzzyMatchScore(
            timestampPending = pending.timestamp,
            blockchainTypeUid = pending.blockchainType.uid,
            amountAtomic = pending.amount.movePointRight(pending.token.decimals).toBigInteger()
                .toString(),
            toAddress = pending.to?.firstOrNull() ?: "",
            real = real
        )
    }

    fun isMatchingRealRecord(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): Boolean = matchScoreForRealRecord(pending, real).isMatch

    private fun calculateFuzzyMatchScore(
        timestampPending: Long,
        blockchainTypeUid: String,
        amountAtomic: String,
        toAddress: String,
        real: TransactionRecord
    ): MatchScore {
        val blockchainMatches = blockchainTypeUid == real.blockchainType.uid
        val amountMatches = compareAmounts(amountAtomic, real)
        val timestampMatches = compareTimestamps(timestampPending, real.timestamp)
        val realTo = real.to?.firstOrNull()

        if (blockchainMatches && amountMatches && timestampMatches) {
            val addressMatches = realTo != null && toAddress.isNotEmpty() &&
                toAddress.equals(realTo, ignoreCase = true)

            return MatchScore(
                isMatch = true,
                confidence = if (addressMatches) 0.9 else 0.8
            )
        }

        return MatchScore(isMatch = false, confidence = 0.0)
    }

    fun calculateMatchScore(
        pending: PendingTransactionRecord,
        real: TransactionRecord
    ): MatchScore = matchScoreForRealRecord(pending, real)

    private fun TransactionRecord.isOutgoingForPendingMatch(): Boolean {
        return when (transactionRecordType) {
            TransactionRecordType.BITCOIN_OUTGOING,
            TransactionRecordType.EVM_OUTGOING,
            TransactionRecordType.EVM_SWAP,
            TransactionRecordType.EVM_UNKNOWN_SWAP,
            TransactionRecordType.TRON_OUTGOING,
            TransactionRecordType.SOLANA_OUTGOING,
            TransactionRecordType.MONERO_OUTGOING,
            TransactionRecordType.STELLAR_OUTGOING -> true

            TransactionRecordType.TON -> to != null && from == null
            else -> false
        }
    }

    private fun TransactionRecord.assetTokenForPendingMatch(): Token? {
        return (getRealValue(this) as? TransactionValue.CoinValue)?.token
            ?: token
    }

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
        return getRealValue(real)?.decimals ?: real.token.decimals
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
        return getRealValue(real)?.decimalValue
    }

    private fun getRealValue(real: TransactionRecord): TransactionValue? {
        if (real is TonTransactionRecord) {
            return real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Swap }
                ?.let { action ->
                    (action.type as? TonTransactionRecord.Action.Type.Swap)?.valueIn
                }
                ?: real.actions.firstOrNull { it.type is TonTransactionRecord.Action.Type.Send }
                    ?.let { action ->
                        (action.type as? TonTransactionRecord.Action.Type.Send)?.value
                    }
        }

        if (real is EvmTransactionRecord) {
            return real.valueIn ?: real.mainValue
        }

        return real.mainValue
    }
}

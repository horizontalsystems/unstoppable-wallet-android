package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

/**
 * Address Poisoning Detection using a Scoring System.
 *
 * Scoring Rules (threshold > 6 points = SPAM):
 * - Zero Value Transfer (transferFrom with value == 0): Automatic SPAM
 * - Zero Value Input (Native): +4 points
 * - Dust Amount: +3 points if < Limit, +2 points if < 5x Limit
 * - Mimic Address (prefix 3 + suffix 3 match, excluding 0x): +4 points
 * - Time Correlation: +4 points if within 5 blocks, +3 points if within 20 minutes
 */
class PoisoningScorer {

    companion object {
        const val SPAM_THRESHOLD = 6

        // Scoring points
        const val POINTS_ZERO_NATIVE_VALUE = 4
        const val POINTS_DUST_BELOW_LIMIT = 3
        const val POINTS_DUST_BELOW_5X_LIMIT = 2
        const val POINTS_MIMIC_ADDRESS = 4
        const val POINTS_TIME_WITHIN_5_BLOCKS = 4
        const val POINTS_TIME_WITHIN_20_MINUTES = 3

        // Time constants
        const val TWENTY_MINUTES_SECONDS = 20 * 60L
        const val BLOCK_COUNT_THRESHOLD = 5

        // Address matching constants
        const val PREFIX_LENGTH = 3
        const val SUFFIX_LENGTH = 3
    }

    /**
     * Data class representing a recent outgoing transaction for comparison
     */
    data class OutgoingTxInfo(
        val recipientAddress: String,
        val timestamp: Long,
        val blockHeight: Int?
    )

    /**
     * Result of spam scoring for a transfer event
     */
    data class ScoringResult(
        val address: String?,
        val score: Int,
        val isAutoSpam: Boolean,
        val reasons: List<String>
    ) {
        val isSpam: Boolean
            get() = isAutoSpam || score > SPAM_THRESHOLD
    }

    /**
     * Calculate spam score for incoming transfer events against recent outgoing transactions.
     *
     * @param events List of incoming transfer events to check
     * @param incomingTimestamp Timestamp of the incoming transaction
     * @param incomingBlockHeight Block height of the incoming transaction (nullable)
     * @param recentOutgoingTxs Recent outgoing transactions for comparison
     * @return List of addresses identified as spam
     */
    fun detectSpamAddresses(
        events: List<TransferEvent>,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<OutgoingTxInfo>
    ): List<String> {
        val spamAddresses = mutableListOf<String>()
        val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits

        // Handle native token events separately (sum them up)
        val nativeEvents = mutableListOf<TransferEvent>()
        val tokenEvents = mutableListOf<TransferEvent>()

        events.forEach { event ->
            if (event.value is TransactionValue.CoinValue && event.value.token.type == TokenType.Native) {
                nativeEvents.add(event)
            } else {
                tokenEvents.add(event)
            }
        }

        // Process token events
        tokenEvents.forEach { event ->
            val result = scoreEvent(
                event = event,
                incomingTimestamp = incomingTimestamp,
                incomingBlockHeight = incomingBlockHeight,
                recentOutgoingTxs = recentOutgoingTxs,
                spamCoinLimits = spamCoinLimits
            )
            if (result.isSpam && result.address != null) {
                spamAddresses.add(result.address)
            }
        }

        // Process native events (sum values)
        if (nativeEvents.isNotEmpty()) {
            val totalNativeValue = nativeEvents.sumOf {
                (it.value as? TransactionValue.CoinValue)?.value ?: BigDecimal.ZERO
            }
            val nativeAddresses = nativeEvents.mapNotNull { it.address }

            if (nativeAddresses.isNotEmpty()) {
                // Use the first native event as representative for scoring
                val representativeEvent = nativeEvents.first()
                val nativeResult = scoreNativeTransfer(
                    totalValue = totalNativeValue,
                    addresses = nativeAddresses,
                    coinCode = (representativeEvent.value as? TransactionValue.CoinValue)?.coinCode ?: "",
                    incomingTimestamp = incomingTimestamp,
                    incomingBlockHeight = incomingBlockHeight,
                    recentOutgoingTxs = recentOutgoingTxs,
                    spamCoinLimits = spamCoinLimits
                )
                if (nativeResult.isSpam) {
                    spamAddresses.addAll(nativeAddresses)
                }
            }
        }

        return spamAddresses.distinct()
    }

    /**
     * Score an individual transfer event
     */
    private fun scoreEvent(
        event: TransferEvent,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<OutgoingTxInfo>,
        spamCoinLimits: Map<String, BigDecimal>
    ): ScoringResult {
        val address = event.address ?: return ScoringResult(null, 0, false, emptyList())
        val value = event.value
        var score = 0
        val reasons = mutableListOf<String>()

        // Check for automatic spam conditions (TokenValue, RawValue)
        when (value) {
            is TransactionValue.TokenValue,
            is TransactionValue.RawValue -> {
                return ScoringResult(address, 0, true, listOf("Unknown token type"))
            }
            is TransactionValue.NftValue -> {
                // NFT with zero value is spam
                if (value.value <= BigDecimal.ZERO) {
                    return ScoringResult(address, 0, true, listOf("Zero-value NFT transfer"))
                }
                return ScoringResult(address, 0, false, emptyList())
            }
            else -> { /* Continue scoring */ }
        }

        val decimalValue = value.decimalValue ?: BigDecimal.ZERO
        val coinCode = value.coinCode
        val limit = spamCoinLimits[coinCode] ?: BigDecimal.ZERO

        // Zero value transfer check (automatic spam for tokens)
        if (decimalValue == BigDecimal.ZERO) {
            return ScoringResult(address, 0, true, listOf("Zero-value token transfer"))
        }

        // Dust amount scoring
        if (limit > BigDecimal.ZERO) {
            when {
                decimalValue < limit -> {
                    score += POINTS_DUST_BELOW_LIMIT
                    reasons.add("Dust: value < limit ($decimalValue < $limit)")
                }
                decimalValue < limit.multiply(BigDecimal("5")) -> {
                    score += POINTS_DUST_BELOW_5X_LIMIT
                    reasons.add("Low value: value < 5x limit")
                }
            }
        }

        // Time and mimic correlation scoring (add points only once per category)
        // Order: fastest checks first (time/block), slowest last (mimic)
        var hasTimeCorrelation = false
        var hasBlockCorrelation = false
        var hasMimicMatch = false

        for (outgoingTx in recentOutgoingTxs) {
            // Time correlation - within 20 minutes (fastest check - simple long comparison)
            if (!hasTimeCorrelation) {
                val timeDiff = kotlin.math.abs(incomingTimestamp - outgoingTx.timestamp)
                if (timeDiff <= TWENTY_MINUTES_SECONDS) {
                    score += POINTS_TIME_WITHIN_20_MINUTES
                    reasons.add("Time correlation: within ${timeDiff}s")
                    hasTimeCorrelation = true
                    if (score > SPAM_THRESHOLD) {
                        return ScoringResult(address, score, false, reasons)
                    }
                }
            }

            // Block correlation - within 5 blocks (fast - integer comparison)
            if (!hasBlockCorrelation && incomingBlockHeight != null && outgoingTx.blockHeight != null) {
                val blockDiff = kotlin.math.abs(incomingBlockHeight - outgoingTx.blockHeight)
                if (blockDiff <= BLOCK_COUNT_THRESHOLD) {
                    score += POINTS_TIME_WITHIN_5_BLOCKS
                    reasons.add("Time correlation: within $blockDiff blocks")
                    hasBlockCorrelation = true
                    if (score > SPAM_THRESHOLD) {
                        return ScoringResult(address, score, false, reasons)
                    }
                }
            }

            // Mimic address check (slowest - string operations, do last)
            if (!hasMimicMatch && isMimicAddress(address, outgoingTx.recipientAddress)) {
                score += POINTS_MIMIC_ADDRESS
                reasons.add("Mimic address: similar to ${outgoingTx.recipientAddress}")
                hasMimicMatch = true
                if (score > SPAM_THRESHOLD) {
                    return ScoringResult(address, score, false, reasons)
                }
            }

            // Early exit if all categories matched
            if (hasTimeCorrelation && hasBlockCorrelation && hasMimicMatch) break
        }

        return ScoringResult(address, score, false, reasons)
    }

    /**
     * Score native token transfers (summed)
     */
    private fun scoreNativeTransfer(
        totalValue: BigDecimal,
        addresses: List<String>,
        coinCode: String,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<OutgoingTxInfo>,
        spamCoinLimits: Map<String, BigDecimal>
    ): ScoringResult {
        var score = 0
        val reasons = mutableListOf<String>()
        val limit = spamCoinLimits[coinCode] ?: BigDecimal.ZERO

        // Zero native value
        if (totalValue == BigDecimal.ZERO) {
            score += POINTS_ZERO_NATIVE_VALUE
            reasons.add("Zero native value")
        }

        // Dust amount scoring
        if (limit > BigDecimal.ZERO && totalValue > BigDecimal.ZERO) {
            when {
                totalValue < limit -> {
                    score += POINTS_DUST_BELOW_LIMIT
                    reasons.add("Dust: native value < limit ($totalValue < $limit)")
                }
                totalValue < limit.multiply(BigDecimal("5")) -> {
                    score += POINTS_DUST_BELOW_5X_LIMIT
                    reasons.add("Low native value: value < 5x limit")
                }
            }
        }

        // Early exit if already spam from zero/dust alone
        if (score > SPAM_THRESHOLD) {
            return ScoringResult(addresses.firstOrNull(), score, false, reasons)
        }

        // Check each address for time/mimic correlation (add points only once per category)
        // Order: fastest checks first (time/block), slowest last (mimic)
        var hasTimeCorrelation = false
        var hasBlockCorrelation = false
        var hasMimicMatch = false

        outer@ for (address in addresses) {
            for (outgoingTx in recentOutgoingTxs) {
                // Time correlation - within 20 minutes (fastest check)
                if (!hasTimeCorrelation) {
                    val timeDiff = kotlin.math.abs(incomingTimestamp - outgoingTx.timestamp)
                    if (timeDiff <= TWENTY_MINUTES_SECONDS) {
                        score += POINTS_TIME_WITHIN_20_MINUTES
                        reasons.add("Time correlation: within ${timeDiff}s")
                        hasTimeCorrelation = true
                        if (score > SPAM_THRESHOLD) {
                            return ScoringResult(addresses.firstOrNull(), score, false, reasons)
                        }
                    }
                }

                // Block correlation - within 5 blocks (fast)
                if (!hasBlockCorrelation && incomingBlockHeight != null && outgoingTx.blockHeight != null) {
                    val blockDiff = kotlin.math.abs(incomingBlockHeight - outgoingTx.blockHeight)
                    if (blockDiff <= BLOCK_COUNT_THRESHOLD) {
                        score += POINTS_TIME_WITHIN_5_BLOCKS
                        reasons.add("Time correlation: within $blockDiff blocks")
                        hasBlockCorrelation = true
                        if (score > SPAM_THRESHOLD) {
                            return ScoringResult(addresses.firstOrNull(), score, false, reasons)
                        }
                    }
                }

                // Mimic address check (slowest - string operations, do last)
                if (!hasMimicMatch && isMimicAddress(address, outgoingTx.recipientAddress)) {
                    score += POINTS_MIMIC_ADDRESS
                    reasons.add("Mimic address: $address similar to ${outgoingTx.recipientAddress}")
                    hasMimicMatch = true
                    if (score > SPAM_THRESHOLD) {
                        return ScoringResult(addresses.firstOrNull(), score, false, reasons)
                    }
                }

                // Early exit if all categories matched
                if (hasTimeCorrelation && hasBlockCorrelation && hasMimicMatch) break@outer
            }
        }

        return ScoringResult(addresses.firstOrNull(), score, false, reasons)
    }

    /**
     * Check if an address mimics another address (same prefix and suffix but different address).
     * Excludes common prefixes like "0x" from comparison.
     *
     * @param incomingAddress The incoming transaction sender address
     * @param outgoingRecipient The outgoing transaction recipient address
     * @return true if the addresses match the mimic pattern
     */
    fun isMimicAddress(incomingAddress: String, outgoingRecipient: String): Boolean {
        // Normalize addresses (remove 0x prefix if present, lowercase)
        val normalizedIncoming = normalizeAddress(incomingAddress)
        val normalizedOutgoing = normalizeAddress(outgoingRecipient)

        // Addresses must be different
        if (normalizedIncoming.equals(normalizedOutgoing, ignoreCase = true)) {
            return false
        }

        // Must have enough characters for comparison
        if (normalizedIncoming.length < PREFIX_LENGTH + SUFFIX_LENGTH ||
            normalizedOutgoing.length < PREFIX_LENGTH + SUFFIX_LENGTH) {
            return false
        }

        // Check prefix match (first 3 characters)
        val incomingPrefix = normalizedIncoming.take(PREFIX_LENGTH)
        val outgoingPrefix = normalizedOutgoing.take(PREFIX_LENGTH)

        // Check suffix match (last 3 characters)
        val incomingSuffix = normalizedIncoming.takeLast(SUFFIX_LENGTH)
        val outgoingSuffix = normalizedOutgoing.takeLast(SUFFIX_LENGTH)

        return incomingPrefix.equals(outgoingPrefix, ignoreCase = true) &&
                incomingSuffix.equals(outgoingSuffix, ignoreCase = true)
    }

    /**
     * Normalize address by removing common prefixes
     */
    private fun normalizeAddress(address: String): String {
        return when {
            address.startsWith("0x", ignoreCase = true) -> address.substring(2)
            address.startsWith("T") && address.length == 34 -> address.substring(1) // Tron
            else -> address
        }
    }
}

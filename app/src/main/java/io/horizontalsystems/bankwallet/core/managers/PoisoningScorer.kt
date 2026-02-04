package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Address Poisoning Detection using a Scoring System.
 *
 * Scoring Rules:
 * - spam ≥ 7 points
 * - suspicious ≥ 3 points
 * - trusted < 3 points
 *
 * Points:
 * - Zero Value Transfer: +4 points
 * - Zero Value NFT: +3 points
 * - Dust Amount (based on risk threshold from config):
 *   - If value < spam (risk/10): +7 points (auto-spam)
 *   - If value < risk: +3 points
 *   - If value < danger (risk*5): +2 points
 * - Address Prefix Match (3 chars): +4 points
 * - Address Suffix Match (3 chars): +4 points
 * - Time Correlation (mutually exclusive): +4 points if within 5 blocks OR +3 points if within 20 minutes
 * - Unknown ERC-20 token: +7 points
 */
class PoisoningScorer {

    companion object {
        const val SPAM_THRESHOLD = 7
        const val SUSPICIOUS_THRESHOLD = 3

        // Scoring points
        const val POINTS_ZERO_VALUE = 4
        const val POINTS_ZERO_NFT = 3
        const val POINTS_MICRO_DUST = 7
        const val POINTS_DUST_BELOW_LIMIT = 3
        const val POINTS_DUST_BELOW_5X_LIMIT = 2
        const val POINTS_ADDRESS_PREFIX_MATCH = 4
        const val POINTS_ADDRESS_SUFFIX_MATCH = 4
        const val POINTS_TIME_WITHIN_5_BLOCKS = 4
        const val POINTS_TIME_WITHIN_20_MINUTES = 3
        const val POINTS_UNKNOWN_TOKEN = 7

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
        val reasons: List<String>
    ) {
        val isSpam: Boolean
            get() = score >= SPAM_THRESHOLD

        val isSuspicious: Boolean
            get() = score >= SUSPICIOUS_THRESHOLD && score < SPAM_THRESHOLD

        val isTrusted: Boolean
            get() = score < SUSPICIOUS_THRESHOLD
    }

    /**
     * Result of spam scoring for a transaction (aggregated from all events)
     */
    data class SpamScoringResult(
        val score: Int,
        val spamAddress: String?
    )

    /**
     * Calculate spam score for incoming transfer events.
     * Returns the maximum score found and the corresponding spam address.
     *
     * @param events List of incoming transfer events to check
     * @param incomingTimestamp Timestamp of the incoming transaction
     * @param incomingBlockHeight Block height of the incoming transaction (nullable)
     * @param recentOutgoingTxs Recent outgoing transactions for comparison
     * @return SpamScoringResult with max score and spam address
     */
    fun calculateSpamScore(
        events: List<TransferEvent>,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<OutgoingTxInfo>
    ): SpamScoringResult {
        val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits
        var maxScore = 0
        var maxScoreAddress: String? = null

        // Score each event individually
        events.forEach { event ->
            val result = scoreEvent(
                event = event,
                incomingTimestamp = incomingTimestamp,
                incomingBlockHeight = incomingBlockHeight,
                recentOutgoingTxs = recentOutgoingTxs,
                spamCoinLimits = spamCoinLimits
            )
            if (result.score > maxScore) {
                maxScore = result.score
                maxScoreAddress = result.address
            }
        }

        return SpamScoringResult(maxScore, maxScoreAddress)
    }

    /**
     * Result of correlation scoring (temporal + mimic)
     */
    private data class CorrelationScore(
        val points: Int,
        val reasons: List<String>
    )

    /**
     * Calculate temporal and address similarity score for addresses against outgoing transactions.
     * Block and Time correlations are mutually exclusive - block is checked first (more precise).
     * Prefix and Suffix matches are scored separately (+4 each).
     */
    private fun calculateCorrelationScore(
        addresses: List<String>,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<OutgoingTxInfo>
    ): CorrelationScore {
        var points = 0
        val reasons = mutableListOf<String>()
        var hasTemporalCorrelation = false
        var hasPrefixMatch = false
        var hasSuffixMatch = false

        outer@ for (address in addresses) {
            for (outgoingTx in recentOutgoingTxs) {
                // Block correlation - within 5 blocks (more precise, check first)
                if (!hasTemporalCorrelation && incomingBlockHeight != null && outgoingTx.blockHeight != null) {
                    val blockDiff = abs(incomingBlockHeight - outgoingTx.blockHeight)
                    if (blockDiff <= BLOCK_COUNT_THRESHOLD) {
                        points += POINTS_TIME_WITHIN_5_BLOCKS
                        reasons.add("Block correlation: within $blockDiff blocks")
                        hasTemporalCorrelation = true
                    }
                }

                // Time correlation - within 20 minutes (only if no block correlation found)
                if (!hasTemporalCorrelation) {
                    val timeDiff = abs(incomingTimestamp - outgoingTx.timestamp)
                    if (timeDiff <= TWENTY_MINUTES_SECONDS) {
                        points += POINTS_TIME_WITHIN_20_MINUTES
                        reasons.add("Time correlation: within ${timeDiff}s")
                        hasTemporalCorrelation = true
                    }
                }

                // Address prefix/suffix check - score separately
                if (!hasPrefixMatch || !hasSuffixMatch) {
                    val (prefixMatch, suffixMatch) = checkAddressSimilarity(address, outgoingTx.recipientAddress)
                    if (prefixMatch && !hasPrefixMatch) {
                        points += POINTS_ADDRESS_PREFIX_MATCH
                        reasons.add("Address prefix match: $address")
                        hasPrefixMatch = true
                    }
                    if (suffixMatch && !hasSuffixMatch) {
                        points += POINTS_ADDRESS_SUFFIX_MATCH
                        reasons.add("Address suffix match: $address")
                        hasSuffixMatch = true
                    }
                }

                // Early exit if we've reached spam threshold
                if (points >= SPAM_THRESHOLD) break@outer
            }
        }

        return CorrelationScore(points, reasons)
    }

    /**
     * Check if addresses have matching prefix and/or suffix.
     * Returns Pair(prefixMatch, suffixMatch).
     */
    private fun checkAddressSimilarity(incomingAddress: String, outgoingRecipient: String): Pair<Boolean, Boolean> {
        val normalizedIncoming = normalizeAddress(incomingAddress)
        val normalizedOutgoing = normalizeAddress(outgoingRecipient)

        // Addresses must be different
        if (normalizedIncoming.equals(normalizedOutgoing, ignoreCase = true)) {
            return Pair(false, false)
        }

        // Must have enough characters for comparison
        if (normalizedIncoming.length < PREFIX_LENGTH + SUFFIX_LENGTH ||
            normalizedOutgoing.length < PREFIX_LENGTH + SUFFIX_LENGTH) {
            return Pair(false, false)
        }

        val prefixMatch = normalizedIncoming.take(PREFIX_LENGTH)
            .equals(normalizedOutgoing.take(PREFIX_LENGTH), ignoreCase = true)
        val suffixMatch = normalizedIncoming.takeLast(SUFFIX_LENGTH)
            .equals(normalizedOutgoing.takeLast(SUFFIX_LENGTH), ignoreCase = true)

        return Pair(prefixMatch, suffixMatch)
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
        val address = event.address ?: return ScoringResult(null, 0, emptyList())
        val value = event.value
        var score = 0
        val reasons = mutableListOf<String>()

        // Check for high-score spam conditions (TokenValue, RawValue)
        when (value) {
            is TransactionValue.TokenValue,
            is TransactionValue.RawValue -> {
                return ScoringResult(address, POINTS_UNKNOWN_TOKEN, listOf("Unknown token type"))
            }
            is TransactionValue.NftValue -> {
                // NFT with zero value gets +3 points
                if (value.value <= BigDecimal.ZERO) {
                    score += POINTS_ZERO_NFT
                    reasons.add("Zero-value NFT transfer")
                    // Continue to check correlation for additional points
                } else {
                    return ScoringResult(address, 0, emptyList())
                }
            }
            else -> { /* Continue scoring */ }
        }

        val decimalValue = value.decimalValue ?: BigDecimal.ZERO
        val coinCode = value.coinCode
        val limit = spamCoinLimits[coinCode] ?: BigDecimal.ZERO

        // Zero value transfer check (+4 points for tokens)
        if (decimalValue == BigDecimal.ZERO && value !is TransactionValue.NftValue) {
            score += POINTS_ZERO_VALUE
            reasons.add("Zero-value token transfer")
        }

        // Dust amount scoring (based on risk threshold)
        if (limit > BigDecimal.ZERO && decimalValue > BigDecimal.ZERO) {
            val spamThreshold = limit.divide(BigDecimal.TEN)
            val dangerThreshold = limit.multiply(BigDecimal("5"))

            when {
                decimalValue < spamThreshold -> {
                    score += POINTS_MICRO_DUST
                    reasons.add("Micro dust: value < spam threshold ($decimalValue < $spamThreshold)")
                }
                decimalValue < limit -> {
                    score += POINTS_DUST_BELOW_LIMIT
                    reasons.add("Dust: value < risk ($decimalValue < $limit)")
                }
                decimalValue < dangerThreshold -> {
                    score += POINTS_DUST_BELOW_5X_LIMIT
                    reasons.add("Low value: value < danger ($decimalValue < $dangerThreshold)")
                }
            }
        }

        // Calculate correlation score
        val correlation = calculateCorrelationScore(listOf(address), incomingTimestamp, incomingBlockHeight, recentOutgoingTxs)
        score += correlation.points
        reasons.addAll(correlation.reasons)

        return ScoringResult(address, score, reasons)
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

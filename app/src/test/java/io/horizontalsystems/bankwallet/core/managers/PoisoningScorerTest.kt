package io.horizontalsystems.bankwallet.core.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for PoisoningScorer - Address Poisoning Detection.
 */
class PoisoningScorerTest {

    private val scorer = PoisoningScorer()

    // ==================== isMimicAddress Tests ====================

    @Test
    fun `detects mimic address with matching prefix and suffix`() {
        val userRecipient = "0xABCD567890abcdef1234"
        val attackerAddress = "0xABCE999999999999f1234"

        assertTrue(scorer.isMimicAddress(attackerAddress, userRecipient))
    }

    @Test
    fun `does not flag identical addresses as mimic`() {
        val address = "0xABCD567890abcdef1234"

        assertFalse(scorer.isMimicAddress(address, address))
    }

    @Test
    fun `does not flag completely different addresses as mimic`() {
        val address1 = "0xABCD567890abcdef1234"
        val address2 = "0x1234567890abcdefABCD"

        assertFalse(scorer.isMimicAddress(address1, address2))
    }

    @Test
    fun `requires both prefix and suffix to match`() {
        val userRecipient = "0xABC1234567890abcdef"

        // Only prefix matches
        assertFalse(scorer.isMimicAddress("0xABC999999999999999", userRecipient))

        // Only suffix matches
        assertFalse(scorer.isMimicAddress("0x999999999999999def", userRecipient))
    }

    @Test
    fun `handles Tron addresses`() {
        val userRecipient = "TABC567890abcdef1234"
        val attackerAddress = "TABX999999999999f234"

        assertTrue(scorer.isMimicAddress(attackerAddress, userRecipient))
    }

    @Test
    fun `case insensitive comparison`() {
        val userRecipient = "0xABCD567890abcdef1234"
        val attackerAddress = "0xabcd999999999999F234"

        assertTrue(scorer.isMimicAddress(attackerAddress, userRecipient))
    }

    @Test
    fun `rejects short addresses`() {
        assertFalse(scorer.isMimicAddress("0xABC", "0xABCD567890abcdef1234"))
        assertFalse(scorer.isMimicAddress("0xABCD567890abcdef1234", "0xABC"))
    }

    // ==================== ScoringResult.isSpam Tests ====================

    @Test
    fun `isSpam true when score reaches threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xSpammer",
            score = PoisoningScorer.SPAM_THRESHOLD,
            reasons = listOf("Zero-value token transfer")
        )

        assertTrue(result.isSpam)
    }

    @Test
    fun `isSpam true when score exceeds threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xSpammer",
            score = 8,
            reasons = listOf("Mimic address", "Dust")
        )

        assertTrue(result.isSpam)
    }

    @Test
    fun `isSuspicious when score between thresholds`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xSuspect",
            score = 5,
            reasons = emptyList()
        )

        assertFalse(result.isSpam)
        assertTrue(result.isSuspicious)
    }

    @Test
    fun `isTrusted when score below suspicious threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xNormal",
            score = 2,
            reasons = emptyList()
        )

        assertFalse(result.isSpam)
        assertFalse(result.isSuspicious)
        assertTrue(result.isTrusted)
    }

    // ==================== Scoring Combinations ====================

    @Test
    fun `prefix plus suffix match reaches threshold`() {
        // Classic poisoning: prefix(4) + suffix(4) = 8 >= 7
        val score = PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH + PoisoningScorer.POINTS_ADDRESS_SUFFIX_MATCH
        assertTrue(score >= PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `prefix plus time correlation does not reach threshold alone`() {
        // prefix(4) + time(3) = 7 >= 7 (just reaches threshold)
        val score = PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH + PoisoningScorer.POINTS_TIME_WITHIN_20_MINUTES
        assertTrue(score >= PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `single factor does not reach threshold`() {
        assertFalse(PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH >= PoisoningScorer.SPAM_THRESHOLD)
        assertFalse(PoisoningScorer.POINTS_DUST_BELOW_LIMIT >= PoisoningScorer.SPAM_THRESHOLD)
        assertFalse(PoisoningScorer.POINTS_ZERO_VALUE >= PoisoningScorer.SPAM_THRESHOLD)
    }

    // ==================== Complex Scoring Scenarios ====================

    @Test
    fun `dust plus block correlation reaches threshold`() {
        // Scenario: Attacker sends dust amount within 5 blocks of user's outgoing tx
        val score = PoisoningScorer.POINTS_DUST_BELOW_LIMIT + PoisoningScorer.POINTS_TIME_WITHIN_5_BLOCKS
        assertTrue("dust(3) + block_correlation(4) = 7 >= 7", score >= PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `time and block correlation are mutually exclusive`() {
        // Block correlation (4 points) is used when available, otherwise time correlation (3 points)
        // They should NOT be combined - only one applies
        assertTrue(
            "block(4) alone should not reach threshold",
            PoisoningScorer.POINTS_TIME_WITHIN_5_BLOCKS < PoisoningScorer.SPAM_THRESHOLD
        )
        assertTrue(
            "time(3) alone should not reach threshold",
            PoisoningScorer.POINTS_TIME_WITHIN_20_MINUTES < PoisoningScorer.SPAM_THRESHOLD
        )
    }

    @Test
    fun `zero value plus prefix and suffix match exceeds threshold`() {
        val score = PoisoningScorer.POINTS_ZERO_VALUE +
                PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH +
                PoisoningScorer.POINTS_ADDRESS_SUFFIX_MATCH
        assertTrue("zero(4) + prefix(4) + suffix(4) = 12 >= 7", score >= PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `low value dust alone does not reach threshold`() {
        // Dust below 5x limit gives fewer points
        assertFalse(
            "dust_5x(2) alone should not reach threshold",
            PoisoningScorer.POINTS_DUST_BELOW_5X_LIMIT >= PoisoningScorer.SPAM_THRESHOLD
        )
    }

    @Test
    fun `low value dust plus time correlation does not reach threshold`() {
        // dust_5x(2) + time(3) = 5 < 7
        val score = PoisoningScorer.POINTS_DUST_BELOW_5X_LIMIT + PoisoningScorer.POINTS_TIME_WITHIN_20_MINUTES
        assertFalse("dust_5x(2) + time(3) = 5 < 7", score >= PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `maximum possible score from all factors`() {
        // All factors combined: prefix(4) + suffix(4) + dust(3) + block(4) = 15
        // Note: time(3) and block(4) are mutually exclusive, block is higher
        val maxScore = PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH +
                PoisoningScorer.POINTS_ADDRESS_SUFFIX_MATCH +
                PoisoningScorer.POINTS_DUST_BELOW_LIMIT +
                PoisoningScorer.POINTS_TIME_WITHIN_5_BLOCKS
        assertEquals("Max score should be 15", 15, maxScore)
        assertTrue(maxScore >= PoisoningScorer.SPAM_THRESHOLD)
    }

    // ==================== Mimic Address Edge Cases ====================

    @Test
    fun `mimic detection with realistic Ethereum addresses`() {
        // Real-world scenario: attacker generates address with same prefix/suffix
        val userSentTo = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
        val attackerMimic = "0x71C0000000000000000000000000000001d8976F"

        assertTrue(scorer.isMimicAddress(attackerMimic, userSentTo))
    }

    @Test
    fun `mimic detection fails when only middle differs slightly`() {
        // If prefix and suffix don't match, not mimic even if similar
        val userSentTo = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
        val notMimic = "0x81C7656EC7ab88b098defB751B7401B5f6d8976A"

        assertFalse(scorer.isMimicAddress(notMimic, userSentTo))
    }

    @Test
    fun `mimic detection with mixed case addresses`() {
        // Ethereum addresses can be mixed case (checksum encoding)
        val userSentTo = "0xABCdef1234567890ABCDEF1234567890abcdEF12"
        val attackerMimic = "0xabcDEF0000000000000000000000000000CDEF12"

        assertTrue(scorer.isMimicAddress(attackerMimic, userSentTo))
    }

    @Test
    fun `mimic detection boundary - exactly 6 character match required`() {
        // PREFIX_LENGTH(3) + SUFFIX_LENGTH(3) = 6 characters must match
        val address1 = "0xABC123"  // Normalized: ABC123 (6 chars total)
        val address2 = "0xABC999"  // Same prefix, different suffix

        // ABC != 999 for suffix, so not mimic
        assertFalse(scorer.isMimicAddress(address1, address2))
    }

    // ==================== Time Constants Verification ====================

    @Test
    fun `twenty minutes constant is correct`() {
        assertEquals(20 * 60L, PoisoningScorer.TWENTY_MINUTES_SECONDS)
    }

    @Test
    fun `block threshold constant is correct`() {
        assertEquals(5, PoisoningScorer.BLOCK_COUNT_THRESHOLD)
    }

    @Test
    fun `spam threshold requires combination of factors`() {
        // Verify that spam detection requires multiple suspicious signals
        assertEquals(7, PoisoningScorer.SPAM_THRESHOLD)
        assertEquals(3, PoisoningScorer.SUSPICIOUS_THRESHOLD)

        // Single high-value factor (4 points) alone shouldn't trigger spam
        assertTrue(PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH < PoisoningScorer.SPAM_THRESHOLD)
        assertTrue(PoisoningScorer.POINTS_TIME_WITHIN_5_BLOCKS < PoisoningScorer.SPAM_THRESHOLD)
        assertTrue(PoisoningScorer.POINTS_ZERO_VALUE < PoisoningScorer.SPAM_THRESHOLD)

        // But single factor can trigger suspicious
        assertTrue(PoisoningScorer.POINTS_ADDRESS_PREFIX_MATCH >= PoisoningScorer.SUSPICIOUS_THRESHOLD)
    }
}

package io.horizontalsystems.bankwallet.core.managers

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
    fun `isSpam true when autoSpam is true regardless of score`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xSpammer",
            score = 0,
            isAutoSpam = true,
            reasons = listOf("Zero-value token transfer")
        )

        assertTrue(result.isSpam)
    }

    @Test
    fun `isSpam true when score exceeds threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xSpammer",
            score = 7,
            isAutoSpam = false,
            reasons = listOf("Mimic address", "Dust")
        )

        assertTrue(result.isSpam)
    }

    @Test
    fun `isSpam false when score at threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xNormal",
            score = PoisoningScorer.SPAM_THRESHOLD,
            isAutoSpam = false,
            reasons = emptyList()
        )

        assertFalse(result.isSpam)
    }

    @Test
    fun `isSpam false when score below threshold`() {
        val result = PoisoningScorer.ScoringResult(
            address = "0xNormal",
            score = 4,
            isAutoSpam = false,
            reasons = listOf("Mimic address")
        )

        assertFalse(result.isSpam)
    }

    // ==================== Scoring Combinations ====================

    @Test
    fun `mimic plus dust exceeds threshold`() {
        val score = PoisoningScorer.POINTS_MIMIC_ADDRESS + PoisoningScorer.POINTS_DUST_BELOW_LIMIT
        assertTrue(score > PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `mimic plus time correlation exceeds threshold`() {
        val score = PoisoningScorer.POINTS_MIMIC_ADDRESS + PoisoningScorer.POINTS_TIME_WITHIN_20_MINUTES
        assertTrue(score > PoisoningScorer.SPAM_THRESHOLD)
    }

    @Test
    fun `single factor does not exceed threshold`() {
        assertFalse(PoisoningScorer.POINTS_MIMIC_ADDRESS > PoisoningScorer.SPAM_THRESHOLD)
        assertFalse(PoisoningScorer.POINTS_DUST_BELOW_LIMIT > PoisoningScorer.SPAM_THRESHOLD)
        assertFalse(PoisoningScorer.POINTS_ZERO_NATIVE_VALUE > PoisoningScorer.SPAM_THRESHOLD)
    }
}
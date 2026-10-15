package io.horizontalsystems.walletkit.core.adapters.zcash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

// Mirrors iOS ZcashShieldStepTests (a: available, f: full, t: transparent, A: amount), so the
// two apps keep one rule.
class ZcashShieldBalanceTest {

    private val threshold = BigDecimal("0.0004")

    private fun balance(
        a: String,
        f: String,
        t: String,
        shielding: Boolean = false,
        held: String = "0",
    ) = ZcashShieldBalance(
        available = BigDecimal(a),
        full = BigDecimal(f),
        transparent = BigDecimal(t),
        threshold = threshold,
        shielding = shielding,
        heldOutput = BigDecimal(held),
    )

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test
    fun amountWithinSpendableNeedsNoStep() {
        assertNull(balance("2", "2", "5").step(BigDecimal("2")))
    }

    @Test
    fun amountJustAboveSpendableOffersShield() {
        assertEquals(ZcashSpendStep.Shield, balance("2", "2", "5").step(BigDecimal("2.00000001")))
    }

    @Test
    fun amountEqualToBalanceOffersShield() {
        assertEquals(ZcashSpendStep.Shield, balance("2", "2", "5").step(BigDecimal("7")))
    }

    @Test
    fun amountJustAboveBalanceIsInsufficient() {
        assertNull(balance("2", "2", "5").step(BigDecimal("7.00000001")))
    }

    @Test
    fun transparentAtThresholdOffersNothing() {
        val b = balance("2", "2", "0.0004")
        assertDecimal("2", b.balance)
        assertNull(b.step(BigDecimal("2.0001")))
    }

    @Test
    fun transparentJustAboveThresholdOffersShield() {
        val b = balance("2", "2", "0.00040001")
        assertDecimal("2.00040001", b.balance)
        assertEquals(ZcashSpendStep.Shield, b.step(BigDecimal("2.0001")))
    }

    @Test
    fun onlyTransparentOffersShield() {
        val b = balance("0", "0", "5")
        assertDecimal("5", b.balance)
        assertEquals(ZcashSpendStep.Shield, b.step(BigDecimal("3")))
    }

    @Test
    fun unconfirmedPrivateFundsWaitForConfirmation() {
        assertEquals(ZcashSpendStep.Confirming, balance("2", "4", "0").step(BigDecimal("3")))
    }

    @Test
    fun amountEqualToBalanceWaitsForConfirmation() {
        assertEquals(ZcashSpendStep.Confirming, balance("2", "4", "0").step(BigDecimal("4")))
    }

    @Test
    fun changeAfterSendWaitsForConfirmation() {
        assertEquals(ZcashSpendStep.Confirming, balance("0", "3.9998", "0").step(BigDecimal("1")))
    }

    @Test
    fun amountCoveredByConfirmingFundsWaitsEvenWithTransparent() {
        assertEquals(ZcashSpendStep.Confirming, balance("0", "3.4", "2").step(BigDecimal("1")))
    }

    @Test
    fun shieldOfferedWhenConfirmingFundsFallShort() {
        assertEquals(ZcashSpendStep.Shield, balance("0", "3.4", "2").step(BigDecimal("3.5")))
    }

    @Test
    fun sentShieldingBeforeRecordIsShielding() {
        // Broadcast, but the transparent funds are still in the summary.
        assertEquals(ZcashSpendStep.Shielding, balance("2", "2", "5", shielding = true).step(BigDecimal("3")))
    }

    @Test
    fun shieldingTakesPrecedenceOverNewTransparent() {
        assertEquals(ZcashSpendStep.Shielding, balance("2", "7", "1", shielding = true).step(BigDecimal("8")))
    }

    @Test
    fun shieldingWithinSpendableNeedsNoStep() {
        assertNull(balance("2", "7", "0", shielding = true).step(BigDecimal("2")))
    }

    @Test
    fun shieldingAboveBalanceIsInsufficient() {
        assertNull(balance("2", "7", "0", shielding = true).step(BigDecimal("7.00000001")))
    }

    @Test
    fun heldBackIsZeroWithoutHeldOutput() {
        assertDecimal("0", balance("7", "7", "0").heldBack)
    }

    @Test
    fun heldBackIsZeroWhileOutputPending() {
        val b = balance("2", "7", "0", held = "5")
        assertDecimal("0", b.heldBack)
        assertDecimal("2", b.spendable)
    }

    @Test
    fun heldBackLimitsSpendableOnceSummaryCountsOutput() {
        val b = balance("7", "7", "0", shielding = true, held = "5")
        assertDecimal("2", b.spendable)
        assertEquals(ZcashSpendStep.Shielding, b.step(BigDecimal("3")))
    }

    @Test
    fun outputIsHeldUntilTheShieldingIsAnchored() {
        assertFalse(ZcashShieldBalance.isHeld(minedHeight = null, lastBlockHeight = 100))
        assertTrue(ZcashShieldBalance.isHeld(minedHeight = 100, lastBlockHeight = 100))
        assertTrue(ZcashShieldBalance.isHeld(minedHeight = 100, lastBlockHeight = 101))
        assertFalse(ZcashShieldBalance.isHeld(minedHeight = 100, lastBlockHeight = 102))
    }

    @Test
    fun heldOutputKeepsShieldingUntilAnchored() {
        // Past the threshold the shielding is no longer in flight, but its output is still held.
        val b = balance("7", "7", "0", shielding = false, held = "5")
        assertEquals(ZcashSpendStep.Confirming, b.step(BigDecimal("3")))
    }

    @Test
    fun heldBackNeverExceedsAvailable() {
        val b = balance("1", "1", "0", held = "5")
        assertDecimal("1", b.heldBack)
        assertDecimal("0", b.spendable)
    }

    @Test
    fun balanceStaysStableWithHeldBack() {
        assertDecimal("7", balance("7", "7", "0", held = "5").balance)
    }

    @Test
    fun staleAvailableAboveFullKeepsBalanceAtSpendable() {
        assertTrue(balance("3", "2", "0").balance >= BigDecimal("3"))
    }

    @Test
    fun unknownHeightIsNotInFlight() {
        assertFalse(ZcashShieldBalance.isShieldingInFlight(minedHeight = null, lastBlockHeight = 0))
    }

    @Test
    fun unminedIsInFlight() {
        assertTrue(ZcashShieldBalance.isShieldingInFlight(minedHeight = null, lastBlockHeight = 100))
    }

    @Test
    fun minedAboveLastBlockIsInFlight() {
        assertTrue(ZcashShieldBalance.isShieldingInFlight(minedHeight = 105, lastBlockHeight = 100))
    }

    @Test
    fun ninthConfirmationIsStillInFlight() {
        assertTrue(ZcashShieldBalance.isShieldingInFlight(minedHeight = 100, lastBlockHeight = 108))
    }

    @Test
    fun tenthConfirmationEndsShielding() {
        assertFalse(ZcashShieldBalance.isShieldingInFlight(minedHeight = 100, lastBlockHeight = 109))
    }

    @Test
    fun latestOwnTransactionPrefersUnmined() {
        val mined = own("a", minedHeight = 90)
        val unmined = own("b", minedHeight = null, expiryHeight = 140)
        assertEquals(unmined, ZcashOwnTransaction.latest(listOf(mined, unmined), lastBlockHeight = 100))
    }

    @Test
    fun latestOwnTransactionSkipsExpired() {
        val mined = own("a", minedHeight = 90)
        val expired = own("b", minedHeight = null, expiryHeight = 100)
        assertEquals(mined, ZcashOwnTransaction.latest(listOf(mined, expired), lastBlockHeight = 100))
    }

    @Test
    fun latestOwnTransactionBreaksUnminedTieByExpiry() {
        val older = own("a", minedHeight = null, expiryHeight = 130, timestamp = 2)
        val newer = own("b", minedHeight = null, expiryHeight = 140, timestamp = 1)
        assertEquals(newer, ZcashOwnTransaction.latest(listOf(older, newer), lastBlockHeight = 100))
    }

    @Test
    fun latestOwnTransactionSkipsReportedExpired() {
        val mined = own("a", minedHeight = 90)
        val expired = own("b", minedHeight = null, expiryHeight = 140).copy(expired = true)
        assertEquals(mined, ZcashOwnTransaction.latest(listOf(mined, expired), lastBlockHeight = 100))
    }

    private fun own(
        txId: String,
        minedHeight: Long?,
        expiryHeight: Long? = null,
        timestamp: Long = 0,
    ) = ZcashOwnTransaction(
        txId = txId,
        isShielding = true,
        minedHeight = minedHeight,
        expiryHeight = expiryHeight,
        timestamp = timestamp,
        received = BigDecimal.ONE,
    )
}

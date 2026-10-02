package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.xrpkit.models.Amount
import io.horizontalsystems.xrpkit.models.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XrpPaymentAmountTest {

    private val requested = Amount.Xrp.fromDrops(1_000_000_000)
    private val delivered = Amount.Xrp.fromDrops(1_000)

    @Test
    fun partialPayment_showsDeliveredAmount() {
        val tx = payment(validated = true, result = "tesSUCCESS", deliveredAmount = delivered)
        assertEquals(delivered, tx.shownPaymentAmount())
    }

    @Test
    fun successWithoutDeliveredAmount_showsNothing() {
        val tx = payment(validated = true, result = "tesSUCCESS", deliveredAmount = null)
        assertNull(tx.shownPaymentAmount())
    }

    @Test
    fun pending_showsSentAmount() {
        val tx = payment(validated = false, result = null, deliveredAmount = null)
        assertEquals(requested, tx.shownPaymentAmount())
    }

    @Test
    fun failed_showsSentAmount() {
        val tx = payment(validated = true, result = "tecPATH_DRY", deliveredAmount = null, failed = true)
        assertEquals(requested, tx.shownPaymentAmount())
    }

    private fun payment(
        validated: Boolean,
        result: String?,
        deliveredAmount: Amount?,
        failed: Boolean = false,
    ) = Transaction(
        hash = "AB",
        ledgerIndex = if (validated) 1L else null,
        timestamp = 0,
        type = "Payment",
        account = "rSender",
        destination = "rReceiver",
        amount = requested,
        deliveredAmount = deliveredAmount,
        feeDrops = 12,
        sequence = 1,
        destinationTag = null,
        sourceTag = null,
        limitAmount = null,
        result = result,
        validated = validated,
        failed = failed,
        lastLedgerSequence = null,
        memo = null,
    )
}

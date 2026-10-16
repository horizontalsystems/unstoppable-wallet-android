package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SendTransactionServiceZcashTest {

    @Test
    fun wholeBalanceIsLoweredByTheFee() {
        val message = "Error creating transaction proposal: Insufficient balance (have 6147606, need 6167606 including fee)"
        assertEquals(6127606L, SendTransactionServiceZcash.maxSendableZatoshi(message, amount = 6147606))
    }

    @Test
    fun otherErrorsGiveNothing() {
        assertNull(SendTransactionServiceZcash.maxSendableZatoshi("Must scan blocks first", amount = 100))
        assertNull(SendTransactionServiceZcash.maxSendableZatoshi(null, amount = 100))
    }

    @Test
    fun balanceBelowTheFeeGivesNothing() {
        val message = "Insufficient balance (have 5000, need 15000 including fee)"
        assertNull(SendTransactionServiceZcash.maxSendableZatoshi(message, amount = 5000))
    }
}

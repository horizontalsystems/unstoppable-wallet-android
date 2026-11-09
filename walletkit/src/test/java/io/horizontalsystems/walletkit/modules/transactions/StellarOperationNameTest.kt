package io.horizontalsystems.walletkit.modules.transactions

import org.junit.Assert.assertEquals
import org.junit.Test

class StellarOperationNameTest {

    @Test
    fun `a snake case operation reads as a sentence`() {
        assertEquals("Create claimable balance", stellarOperationName("create_claimable_balance"))
    }

    @Test
    fun `a single word is capitalized`() {
        assertEquals("Inflation", stellarOperationName("inflation"))
    }

    @Test
    fun `an empty name stays empty`() {
        assertEquals("", stellarOperationName(""))
    }
}

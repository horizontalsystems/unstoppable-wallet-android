package io.horizontalsystems.walletkit.core.factories

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstUnusedNameTest {

    private val name: (Int) -> String = { "Wallet $it" }

    @Test
    fun startsAtOneWithoutAccounts() {
        assertEquals("Wallet 1", firstUnusedName(emptySet(), name))
    }

    @Test
    fun startsAtOneWhenAccountsHaveOtherNames() {
        assertEquals("Wallet 1", firstUnusedName(setOf("Savings", "Watch Wallet 1"), name))
    }

    @Test
    fun continuesAfterTheNumbersInUse() {
        assertEquals("Wallet 3", firstUnusedName(setOf("Wallet 1", "Wallet 2"), name))
    }

    @Test
    fun fillsTheLowestGap() {
        assertEquals("Wallet 2", firstUnusedName(setOf("Wallet 1", "Wallet 3", "Wallet 4"), name))
    }

    @Test
    fun fallsBackToOneWhenTheTemplateIgnoresTheNumber() {
        assertEquals("Wallet", firstUnusedName(setOf("Wallet"), { "Wallet" }))
    }
}

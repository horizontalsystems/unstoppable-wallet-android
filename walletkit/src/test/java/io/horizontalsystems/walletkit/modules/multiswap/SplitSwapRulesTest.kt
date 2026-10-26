package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.BlockchainType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitSwapRulesTest {

    @Test
    fun `a two-output split is the first share and its complement`() {
        assertEquals(listOf(60, 40), SplitSwapRules.percents(60))
        assertEquals(listOf(50, 50), SplitSwapRules.percents(SplitSwapRules.PERCENT_DEFAULT))
    }

    @Test
    fun `shares must be positive and sum to exactly 100`() {
        assertTrue(SplitSwapRules.isValid(listOf(10, 90)))
        assertTrue(SplitSwapRules.isValid(listOf(33, 33, 34)))
        assertFalse(SplitSwapRules.isValid(listOf(100)))
        assertFalse(SplitSwapRules.isValid(listOf(50, 49)))
        assertFalse(SplitSwapRules.isValid(listOf(0, 100)))
        assertFalse(SplitSwapRules.isValid(listOf(110, -10)))
    }

    @Test
    fun `only a later repeat of an address is a duplicate`() {
        assertEquals(setOf(1), SplitSwapRules.duplicateIndexes(listOf("bc1qa", "bc1qa"), BlockchainType.Bitcoin))
        assertEquals(emptySet<Int>(), SplitSwapRules.duplicateIndexes(listOf("bc1qa", "bc1qb"), BlockchainType.Bitcoin))
        assertEquals(emptySet<Int>(), SplitSwapRules.duplicateIndexes(listOf(null, null), BlockchainType.Bitcoin))
        assertEquals(setOf(2), SplitSwapRules.duplicateIndexes(listOf("a", null, "a"), BlockchainType.Bitcoin))
    }

    @Test
    fun `evm addresses compare without case, other chains with it`() {
        assertEquals(setOf(1), SplitSwapRules.duplicateIndexes(listOf("0xAbC", "0xabc"), BlockchainType.Ethereum))
        assertEquals(emptySet<Int>(), SplitSwapRules.duplicateIndexes(listOf("TJRab", "tjrab"), BlockchainType.Tron))
    }
}

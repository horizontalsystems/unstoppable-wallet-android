package io.horizontalsystems.walletkit.core.adapters.zcash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionBatchOrderTest {

    @Test
    fun `batches finishing in order are all accepted`() {
        val order = TransactionBatchOrder()
        val first = order.next()
        val second = order.next()

        assertTrue(order.accept(first))
        assertTrue(order.accept(second))
    }

    @Test
    fun `a batch is refused once a later one has been stored`() {
        val order = TransactionBatchOrder()
        val slow = order.next()
        val fast = order.next()

        assertTrue(order.accept(fast))
        assertFalse(order.accept(slow))
    }

    @Test
    fun `a later batch with nothing new still supersedes an earlier one`() {
        val order = TransactionBatchOrder()
        val slow = order.next()
        val empty = order.next()

        // The provider accepts a batch with nothing to store as well: the list already matches it.
        assertTrue(order.accept(empty))
        assertFalse(order.accept(slow))
    }

    @Test
    fun `a batch is superseded only once a later one has been stored`() {
        val order = TransactionBatchOrder()
        val slow = order.next()
        val fast = order.next()

        assertFalse(order.isSuperseded(slow))
        assertTrue(order.accept(fast))
        assertTrue(order.isSuperseded(slow))
        assertFalse(order.isSuperseded(fast))
    }

    @Test
    fun `the same batch may be accepted twice`() {
        val order = TransactionBatchOrder()
        val batch = order.next()

        assertTrue(order.accept(batch))
        assertTrue(order.accept(batch))
    }
}

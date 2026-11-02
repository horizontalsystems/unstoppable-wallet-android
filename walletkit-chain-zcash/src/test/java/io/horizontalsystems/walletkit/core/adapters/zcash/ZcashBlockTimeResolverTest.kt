package io.horizontalsystems.walletkit.core.adapters.zcash

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZcashBlockTimeResolverTest {

    @Test
    fun `exact time is returned and cached per height`() = runBlocking {
        var fetches = 0
        val resolver = ZcashBlockTimeResolver(
            fetch = { fetches++; 1_762_856_275L },
            estimate = { error("estimate must not be consulted when the fetch succeeds") },
        )

        assertEquals(1_762_856_275L, resolver.blockTime(3_131_024))
        assertEquals(1_762_856_275L, resolver.blockTime(3_131_024))
        assertEquals(1, fetches)
    }

    @Test
    fun `estimate fills in when the fetch fails and is not cached`() = runBlocking {
        var fetchWorks = false
        var fetches = 0
        val resolver = ZcashBlockTimeResolver(
            fetch = { fetches++; if (fetchWorks) 1_700_000_100L else throw IllegalStateException("offline") },
            estimate = { 1_700_000_000L },
        )

        assertEquals(1_700_000_000L, resolver.blockTime(42))

        // The next call retries the exact source instead of serving the estimate from a cache.
        fetchWorks = true
        assertEquals(1_700_000_100L, resolver.blockTime(42))
        assertEquals(2, fetches)
    }

    @Test
    fun `a failing estimate yields null rather than a 1970 date`() = runBlocking {
        val resolver = ZcashBlockTimeResolver(
            fetch = { null },
            estimate = { throw IllegalArgumentException("height below the first checkpoint") },
        )

        assertNull(resolver.blockTime(7))
    }

    @Test
    fun `non-positive times count as unknown`() = runBlocking {
        val resolver = ZcashBlockTimeResolver(
            fetch = { 0L },
            estimate = { -5L },
        )

        assertNull(resolver.blockTime(100))
    }

    @Test
    fun `heights are cached independently`() = runBlocking {
        val resolver = ZcashBlockTimeResolver(
            fetch = { height -> height * 10 },
            estimate = { null },
        )

        assertEquals(100L, resolver.blockTime(10))
        assertEquals(200L, resolver.blockTime(20))
        assertEquals(100L, resolver.blockTime(10))
    }
}

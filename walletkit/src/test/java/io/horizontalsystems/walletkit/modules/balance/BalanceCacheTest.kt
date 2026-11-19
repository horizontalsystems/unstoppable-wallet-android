package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.storage.EnabledWalletsCacheDao
import io.horizontalsystems.walletkit.entities.EnabledWalletCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCacheTest {

    private class CountingDao : EnabledWalletsCacheDao {
        var reads = 0

        override fun insertAll(list: List<EnabledWalletCache>) = Unit

        override fun getAll(): List<EnabledWalletCache> {
            reads++
            return emptyList()
        }

        override fun flowByAccountId(accountId: String): Flow<List<EnabledWalletCache>> = emptyFlow()
    }

    @Test
    fun `creating the cache does not read the database`() {
        val dao = CountingDao()

        BalanceCache(dao)

        assertEquals(0, dao.reads)
    }

    @Test
    fun `storing balances does not read the database`() {
        val dao = CountingDao()

        BalanceCache(dao).setCache(emptyMap())

        assertEquals(0, dao.reads)
    }
}

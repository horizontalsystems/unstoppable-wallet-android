package cash.p.terminal.modules.balance

import cash.p.terminal.core.storage.EnabledWalletsCacheDao
import cash.p.terminal.entities.EnabledWalletCache
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BalanceCacheTest {

    @Test
    fun getCache_cachedTotalBalance_restoresAsAvailable() {
        val wallet = wallet()
        val dao = mockk<EnabledWalletsCacheDao> {
            every { getAll() } returns listOf(
                EnabledWalletCache(
                    tokenQueryId = wallet.token.tokenQuery.id,
                    accountId = wallet.account.id,
                    balance = BigDecimal("3"),
                    balanceLocked = BigDecimal.ZERO,
                    stackingUnpaid = BigDecimal.ZERO
                )
            )
        }

        val cached = BalanceCache(dao).getCache(wallet)

        assertBigDecimalEquals("3", cached?.available)
        assertBigDecimalEquals("0", cached?.pending)
        assertBigDecimalEquals("3", cached?.total)
    }

    @Test
    fun setCache_pendingBalance_persistsColdTotalInBalance() {
        val wallet = wallet()
        val capturedCache = slot<List<EnabledWalletCache>>()
        val dao = mockk<EnabledWalletsCacheDao> {
            every { getAll() } returns emptyList()
            every { insertAll(capture(capturedCache)) } returns Unit
        }

        BalanceCache(dao).setCache(
            wallet,
            BalanceData(
                available = BigDecimal("1"),
                pending = BigDecimal("2")
            )
        )

        val saved = capturedCache.captured.single()
        assertBigDecimalEquals("3", saved.balance)
    }

    private fun wallet(): Wallet {
        val account = mockk<Account> {
            every { id } returns "account-1"
        }
        val token = Token(
            coin = Coin(uid = "zcash", name = "Zcash", code = "ZEC"),
            blockchain = Blockchain(BlockchainType.Zcash, "Zcash", null),
            type = TokenType.AddressSpecTyped(TokenType.AddressSpecType.Shielded),
            decimals = 8
        )
        return mockk {
            every { this@mockk.account } returns account
            every { this@mockk.token } returns token
        }
    }

    private fun assertBigDecimalEquals(expected: String, actual: BigDecimal?) {
        assertEquals(BigDecimal(expected).stripTrailingZeros(), actual?.stripTrailingZeros())
    }
}

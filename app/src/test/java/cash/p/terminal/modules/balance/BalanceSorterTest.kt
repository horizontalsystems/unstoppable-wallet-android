package cash.p.terminal.modules.balance

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.models.CoinPrice
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BalanceSorterValueTest {

    private val sorter = BalanceSorter()

    @Test
    fun sort_valueSortsByDisplayedTotalBalance() {
        val unconfirmedVisibleBalance = balanceItem(
            uid = "mweb",
            available = BigDecimal.ZERO,
            notRelayed = BigDecimal.TEN,
        )
        val smallerSpendableBalance = balanceItem(
            uid = "public",
            available = BigDecimal("5"),
        )

        val sorted = sorter.sort(
            listOf(smallerSpendableBalance, unconfirmedVisibleBalance),
            BalanceSortType.Value
        )

        assertEquals(listOf("mweb", "public"), sorted.map { it.wallet.coin.uid })
    }

    private fun balanceItem(
        uid: String,
        available: BigDecimal,
        notRelayed: BigDecimal = BigDecimal.ZERO,
    ): BalanceItem {
        val coin = Coin(uid = uid, name = uid, code = uid)
        val token = mockk<Token> {
            every { blockchainType } returns BlockchainType.Litecoin
        }
        val wallet = mockk<Wallet> {
            every { this@mockk.coin } returns coin
            every { this@mockk.token } returns token
        }

        return BalanceItem(
            wallet = wallet,
            balanceData = BalanceData(
                available = available,
                notRelayed = notRelayed,
            ),
            state = AdapterState.Synced,
            sendAllowed = true,
            coinPrice = CoinPrice(
                coinUid = uid,
                currencyCode = "USD",
                value = BigDecimal.ONE,
                diff1h = null,
                diff24h = null,
                diff7d = null,
                diff30d = null,
                diff1y = null,
                diffAll = null,
                timestamp = 0,
            )
        )
    }
}

package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class MultiSwapOnChainMonitorTest {

    @Test
    fun observeBalanceIncrease_pendingOnlyBalanceIncrease_triggersCallback() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val token = zcashToken()
        val wallet = mockk<Wallet> {
            every { coin } returns token.coin
            every { this@mockk.token } returns token
        }
        val walletBalanceUpdatedFlow = MutableSharedFlow<Wallet>(extraBufferCapacity = 1)
        var balanceData = BalanceData(available = BigDecimal.ZERO)
        val adapter = mockk<IBalanceAdapter> {
            every { this@mockk.balanceData } answers { balanceData }
        }
        val walletManager = mockk<IWalletManager> {
            every { activeWallets } returns listOf(wallet)
        }
        val adapterManager = mockk<IAdapterManager> {
            every { getBalanceAdapterForWallet(wallet) } returns adapter
            every { this@mockk.walletBalanceUpdatedFlow } returns walletBalanceUpdatedFlow
        }
        val monitor = MultiSwapOnChainMonitor(
            walletManager = walletManager,
            adapterManager = adapterManager,
            dispatcherProvider = TestDispatcherProvider(dispatcher, this)
        )
        var balanceIncreased = false

        val started = monitor.observeBalanceIncrease(
            coinUid = "zcash",
            blockchainType = BlockchainType.Zcash,
            scope = this,
        ) {
            balanceIncreased = true
        }

        assertTrue(started)

        balanceData = BalanceData(
            available = BigDecimal.ZERO,
            pending = BigDecimal.ONE
        )
        walletBalanceUpdatedFlow.emit(wallet)
        advanceUntilIdle()

        assertTrue(balanceIncreased)
    }

    private fun zcashToken() = Token(
        coin = Coin(uid = "zcash", name = "Zcash", code = "ZEC"),
        blockchain = Blockchain(BlockchainType.Zcash, "Zcash", null),
        type = TokenType.AddressSpecTyped(TokenType.AddressSpecType.Shielded),
        decimals = 8
    )
}

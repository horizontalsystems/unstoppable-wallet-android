package cash.p.terminal.core.storage

import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.ActiveAccountState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class PendingMultiSwapStorageTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(dispatcher)
    private val dao = mockk<PendingMultiSwapDao>(relaxed = true)
    private val storage = PendingMultiSwapStorage(
        dao = dao,
        dispatcherProvider = TestDispatcherProvider(dispatcher, testScope)
    )

    private fun swap(
        id: String = "swap-1",
        accountId: String = "account-1",
    ) = PendingMultiSwap(
        id = id,
        accountId = accountId,
        createdAt = 1000L,
        coinUidIn = "binancecoin",
        blockchainTypeIn = "binance-smart-chain",
        amountIn = BigDecimal("1.0"),
        coinUidIntermediate = "the-open-network",
        blockchainTypeIntermediate = "the-open-network",
        coinUidOut = "pirate",
        blockchainTypeOut = "the-open-network",
        leg1ProviderId = "changenow",
        leg1IsOffChain = true,
        leg1TransactionId = "0xabc",
        leg1AmountOut = null,
        leg1Status = PendingMultiSwap.STATUS_EXECUTING,
        leg2ProviderId = "stonfi",
        leg2IsOffChain = false,
        leg2TransactionId = null,
        leg2AmountOut = null,
        leg2Status = PendingMultiSwap.STATUS_PENDING,
        expectedAmountOut = BigDecimal("100"),
    )

    private fun account(id: String) = Account(
        id = id,
        name = "Test",
        type = AccountType.EvmAddress("0x1"),
        origin = AccountOrigin.Created,
        level = 0,
    )

    @Test
    fun getByAccountId_delegatesToDao() = runTest(dispatcher) {
        val swapsForAccount = listOf(swap(id = "s1", accountId = "acc-A"))
        every { dao.getByAccountId("acc-A") } returns flowOf(swapsForAccount)

        val result = storage.getByAccountId("acc-A").first()

        assertEquals(swapsForAccount, result)
        verify { dao.getByAccountId("acc-A") }
    }

    @Test
    fun getByAccountId_returnsOnlyMatchingAccount() = runTest(dispatcher) {
        every { dao.getByAccountId("acc-A") } returns flowOf(listOf(swap(id = "s1", accountId = "acc-A")))

        val result = storage.getByAccountId("acc-A").first()

        assertEquals(1, result.size)
        assertEquals("acc-A", result.first().accountId)
    }

    @Test
    fun getByAccountId_returnsEmptyForUnknownAccount() = runTest(dispatcher) {
        every { dao.getByAccountId("unknown") } returns flowOf(emptyList())

        val result = storage.getByAccountId("unknown").first()

        assertEquals(emptyList<PendingMultiSwap>(), result)
    }

    @Test
    fun getAllOnceByAccountId_delegatesToDao() = runTest(dispatcher) {
        val swaps = listOf(swap(id = "s1", accountId = "acc-A"))
        coEvery { dao.getAllOnceByAccountId("acc-A") } returns swaps

        val result = storage.getAllOnceByAccountId("acc-A")

        assertEquals(swaps, result)
        coVerify { dao.getAllOnceByAccountId("acc-A") }
    }

    @Test
    fun observeForActiveAccount_emitsSwapsForActiveAccount() = runTest(dispatcher) {
        val swaps = listOf(swap(id = "s1", accountId = "acc-A"))
        every { dao.getByAccountId("acc-A") } returns flowOf(swaps)
        val accountFlow = MutableStateFlow<ActiveAccountState>(
            ActiveAccountState.ActiveAccount(account("acc-A"))
        )

        val result = storage.observeForActiveAccount(accountFlow).first()

        assertEquals(swaps, result)
    }

    @Test
    fun observeForActiveAccount_emitsEmptyWhenNotLoaded() = runTest(dispatcher) {
        val accountFlow = MutableStateFlow<ActiveAccountState>(ActiveAccountState.NotLoaded)

        val result = storage.observeForActiveAccount(accountFlow).first()

        assertEquals(emptyList<PendingMultiSwap>(), result)
    }

    @Test
    fun observeForActiveAccount_emitsEmptyWhenAccountNull() = runTest(dispatcher) {
        val accountFlow = MutableStateFlow<ActiveAccountState>(
            ActiveAccountState.ActiveAccount(null)
        )

        val result = storage.observeForActiveAccount(accountFlow).first()

        assertEquals(emptyList<PendingMultiSwap>(), result)
    }

    @Test
    fun observeForActiveAccount_switchesWhenAccountChanges() = runTest(dispatcher) {
        val swapsA = listOf(swap(id = "s1", accountId = "acc-A"))
        val swapsB = listOf(swap(id = "s2", accountId = "acc-B"))
        every { dao.getByAccountId("acc-A") } returns flowOf(swapsA)
        every { dao.getByAccountId("acc-B") } returns flowOf(swapsB)

        val accountFlow = MutableStateFlow<ActiveAccountState>(
            ActiveAccountState.ActiveAccount(account("acc-A"))
        )

        val flow = storage.observeForActiveAccount(accountFlow)

        assertEquals(swapsA, flow.first())

        accountFlow.value = ActiveAccountState.ActiveAccount(account("acc-B"))

        assertEquals(swapsB, flow.first())
    }
}

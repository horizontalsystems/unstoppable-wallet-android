package cash.p.terminal.core.storage

import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.swaprepository.SwapProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SwapProviderTransactionsStorageTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(dispatcher)
    private val dao = mockk<SwapProviderTransactionsDao>(relaxed = true)
    private val storage = SwapProviderTransactionsStorage(
        dao = dao,
        dispatcherProvider = TestDispatcherProvider(dispatcher, testScope)
    )

    private fun swap(accountId: String) = SwapProviderTransaction(
        date = 1_000L,
        outgoingRecordUid = null,
        transactionId = "tx-1",
        status = TransactionStatusEnum.NEW.name.lowercase(),
        provider = SwapProvider.CHANGENOW,
        coinUidIn = "binancecoin",
        blockchainTypeIn = "binance-smart-chain",
        amountIn = BigDecimal.ONE,
        addressIn = "addr-in",
        coinUidOut = "litecoin",
        blockchainTypeOut = "litecoin",
        amountOut = BigDecimal.TEN,
        addressOut = "addr-out",
        accountId = accountId,
    )

    @Test
    fun observeAllByAccount_delegatesToDaoWithDefaultLimit() = runTest(dispatcher) {
        val transactions = listOf(swap(accountId = "acc-A"))
        every { dao.observeAllByAccount("acc-A", 100) } returns flowOf(transactions)

        val result = storage.observeAllByAccount("acc-A").first()

        assertEquals(transactions, result)
        verify { dao.observeAllByAccount("acc-A", 100) }
    }

    @Test
    fun getAllUnfinishedByAccount_delegatesToDao() {
        val transactions = listOf(swap(accountId = "acc-A"))
        val excluded = SwapProviderTransaction.FINISHED_STATUSES
        every { dao.getAllUnfinishedByAccount("acc-A", excluded, 10) } returns transactions

        val result = storage.getAllUnfinishedByAccount("acc-A", excluded, 10)

        assertEquals(transactions, result)
        verify { dao.getAllUnfinishedByAccount("acc-A", excluded, 10) }
    }
}

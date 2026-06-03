package cash.p.terminal.core.utils

import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.swaprepository.SwapProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class SwapTransactionMatcherTest {

    private val storage = mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val matcher = SwapTransactionMatcher(storage)

    private val onChainAmount = BigDecimal("0.01965841")

    private fun swap() = SwapProviderTransaction(
        date = 1_000L,
        outgoingRecordUid = null,
        transactionId = "tx-1",
        status = TransactionStatusEnum.SENDING.name.lowercase(),
        provider = SwapProvider.CHANGENOW,
        coinUidIn = "binancecoin",
        blockchainTypeIn = "binance-smart-chain",
        amountIn = BigDecimal.ONE,
        addressIn = "addr-in",
        coinUidOut = "litecoin",
        blockchainTypeOut = "litecoin",
        amountOut = BigDecimal("0.0196651"),
        addressOut = "ltc1qzvd",
        accountId = "acc-1",
    )

    private fun incoming(
        addresses: List<String>? = listOf("ltc1qzvd"),
        amount: BigDecimal? = onChainAmount,
    ) = IncomingTransaction(
        uid = "record-uid",
        amount = amount,
        timestamp = 1_000L,
        coinUid = "litecoin",
        blockchainType = "litecoin",
        addresses = addresses,
        accountId = "acc-1",
    )

    @Test
    fun findMatchingSwap_alreadyMatched_returnsCachedWithoutFurtherLookup() {
        val swap = swap()
        every { storage.getByIncomingRecordUid("record-uid") } returns swap

        val result = matcher.findMatchingSwap(incoming())

        assertEquals(swap, result)
        verify(exactly = 0) { storage.getByAddressAndAmount(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun findMatchingSwap_addressMatch_returnsSwapAndCachesIncomingRecordUid() {
        val swap = swap()
        every { storage.getByIncomingRecordUid(any()) } returns null
        every {
            storage.getByAddressAndAmount("ltc1qzvd", "litecoin", "litecoin", "acc-1", any(), any())
        } returns swap

        val result = matcher.findMatchingSwap(incoming())

        assertEquals(swap, result)
        verify {
            storage.setIncomingRecordUid(
                date = swap.date,
                incomingRecordUid = "record-uid",
                amountOutReal = onChainAmount,
            )
        }
    }

    @Test
    fun findMatchingSwap_noAddresses_usesAmountTimestampPathAndCaches() {
        val swap = swap()
        every { storage.getByIncomingRecordUid(any()) } returns null
        every {
            storage.getUnmatchedSwapsByTokenOut(any(), any(), any(), any(), any(), any(), "acc-1", any())
        } returns listOf(swap)

        val result = matcher.findMatchingSwap(incoming(addresses = null))

        assertEquals(swap, result)
        verify { storage.setIncomingRecordUid(swap.date, "record-uid", onChainAmount) }
    }

    @Test
    fun findMatchingSwap_addressMissAmountMiss_fallsBackToTimestampOnly() {
        val swap = swap()
        every { storage.getByIncomingRecordUid(any()) } returns null
        every { storage.getByAddressAndAmount(any(), any(), any(), any(), any(), any()) } returns null
        every { storage.getByTokenOut("litecoin", "litecoin", 1_000L, "acc-1") } returns swap

        val result = matcher.findMatchingSwap(incoming())

        assertEquals(swap, result)
    }

    @Test
    fun findMatchingSwap_nothingFound_returnsNull() {
        every { storage.getByIncomingRecordUid(any()) } returns null
        every { storage.getByAddressAndAmount(any(), any(), any(), any(), any(), any()) } returns null
        every { storage.getByTokenOut(any(), any(), any(), any()) } returns null

        val result = matcher.findMatchingSwap(incoming())

        assertNull(result)
    }
}

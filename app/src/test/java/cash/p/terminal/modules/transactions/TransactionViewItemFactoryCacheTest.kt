package cash.p.terminal.modules.transactions

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.PoisonAddressManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.utils.SwapTransactionMatcher
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewMode
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.helpers.DateHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TransactionViewItemFactoryCacheTest {

    private companion object {
        const val PENDING_UID = "4392cdda-870d-46d7-8cc8-c06ac0be4bd3"
        const val TX_HASH = "0e850ae3cb3963672d2880a4940172732ccab477f0ea3fc93a8914e912c670ad"
        const val PENDING_TIMESTAMP = 1_779_766_789L

        val ZEC_AMOUNT: BigDecimal = BigDecimal("0.01285429")
    }

    private val evmLabelManager = mockk<EvmLabelManager>()
    private val contactsRepository = mockk<ContactsRepository>()
    private val balanceHiddenManager = mockk<BalanceHiddenManager>()
    private val swapProviderTransactionsStorage = mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val swapTransactionMatcher = mockk<SwapTransactionMatcher>(relaxed = true)
    private val numberFormatter = mockk<IAppNumberFormatter>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>()
    private val poisonAddressManager = mockk<PoisonAddressManager>()
    private val accountManager = mockk<IAccountManager>(relaxed = true)
    private val appNumberFormatter = mockk<IAppNumberFormatter>()

    private lateinit var factory: TransactionViewItemFactory

    @Before
    fun setUp() {
        mockkObject(App)
        mockkObject(DateHelper)

        every { App.numberFormatter } returns appNumberFormatter
        every { DateHelper.getOnlyTime(any()) } returns "12:00"
        every { DateHelper.shortDate(any(), any(), any()) } returns "Apr 6"
        every { appNumberFormatter.formatCoinShort(any(), any(), any()) } answers {
            val value = firstArg<BigDecimal>().stripTrailingZeros().toPlainString()
            val code = secondArg<String?>().orEmpty()
            "$code:$value"
        }
        every { appNumberFormatter.formatFiatShort(any(), any(), any()) } returns "$0"

        every { evmLabelManager.mapped(any()) } answers { firstArg<String>() }
        every { contactsRepository.getContactsFiltered(any(), addressQuery = any()) } returns emptyList()
        every { balanceHiddenManager.balanceHidden } returns false
        every { balanceHiddenManager.isTransactionInfoHidden(any()) } returns false
        every { balanceHiddenManager.isTransactionInfoHiddenForWallet(any(), any()) } returns false
        every { localStorage.addressPoisoningViewMode } returns AddressPoisoningViewMode.COMPACT
        coEvery { poisonAddressManager.getPoisonStatus(any<TransactionRecord>()) } returns PoisonStatus.BLOCKCHAIN

        factory = TransactionViewItemFactory(
            evmLabelManager = evmLabelManager,
            contactsRepository = contactsRepository,
            balanceHiddenManager = balanceHiddenManager,
            swapProviderTransactionsStorage = swapProviderTransactionsStorage,
            swapTransactionMatcher = swapTransactionMatcher,
            numberFormatter = numberFormatter,
            marketKit = marketKit,
            localStorage = localStorage,
            poisonAddressManager = poisonAddressManager,
            accountManager = accountManager,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun convertToViewItemCached_updatedListData_rebuildsCachedItem() = runTest {
        val initialRecord = createUnknownSwapRecord(
            uid = "swap-uid",
            valueOut = null,
        )
        val updatedRecord = createUnknownSwapRecord(
            uid = "swap-uid",
            valueOut = TransactionValue.TokenValue(
                tokenName = "USDT",
                tokenCode = "USDT",
                tokenDecimals = 6,
                value = BigDecimal("42"),
            ),
        )

        val initialItem = TransactionItem(
            record = initialRecord,
            currencyValue = null,
            lastBlockInfo = null,
            nftMetadata = emptyMap(),
        )
        val updatedItem = initialItem.withUpdatedListData(
            record = updatedRecord,
        )

        val initialViewItem = factory.convertToViewItemCached(initialItem)
        val updatedViewItem = factory.convertToViewItemCached(updatedItem)

        assertNotEquals(initialItem.cacheVersion, updatedItem.cacheVersion)
        assertNull(initialViewItem.primaryValue)
        assertEquals("+USDT:42", updatedViewItem.primaryValue?.value)
    }

    @Test
    fun convertToViewItemCached_detailsOnlyCopy_reusesCachedItem() = runTest {
        val record = createUnknownSwapRecord(
            uid = "swap-uid",
            valueOut = null,
        )
        val initialItem = TransactionItem(
            record = record,
            currencyValue = null,
            lastBlockInfo = null,
            nftMetadata = emptyMap(),
        )
        val detailsItem = initialItem.copy(
            changeNowTransactionId = "tx-id",
        )

        val initialViewItem = factory.convertToViewItemCached(initialItem)
        val detailsViewItem = factory.convertToViewItemCached(detailsItem)

        assertEquals(initialItem.cacheVersion, detailsItem.cacheVersion)
        assertSame(initialViewItem, detailsViewItem)
    }

    @Test
    fun convertToViewItemCached_pendingOutgoingFallbackWithHash_persistsTransactionHash() = runTest {
        val record = createPendingRecord(
            transactionHash = TX_HASH,
        )
        val swap = createSwapProviderTransaction(outgoingRecordUid = null)

        every { swapProviderTransactionsStorage.getByOutgoingRecordUid(TX_HASH) } returns null
        stubOutgoingFallback(swap)

        val viewItem = factory.convertToViewItemCached(createTransactionItem(record))

        assertEquals(swap.transactionId, viewItem.changeNowTransactionId)
        verify {
            swapProviderTransactionsStorage.setOutgoingRecordUid(
                date = swap.date,
                outgoingRecordUid = TX_HASH,
            )
        }
        verify(exactly = 0) {
            swapProviderTransactionsStorage.setOutgoingRecordUid(
                date = swap.date,
                outgoingRecordUid = record.uid,
            )
        }
    }

    @Test
    fun convertToViewItemCached_pendingOutgoingMatchedByHash_doesNotPersistPendingUid() = runTest {
        val record = createPendingRecord(
            transactionHash = TX_HASH,
        )
        val swap = createSwapProviderTransaction(outgoingRecordUid = TX_HASH)

        every { swapProviderTransactionsStorage.getByOutgoingRecordUid(TX_HASH) } returns swap
        stubOutgoingFallback(swap)

        val viewItem = factory.convertToViewItemCached(createTransactionItem(record))

        assertEquals(swap.transactionId, viewItem.changeNowTransactionId)
        verify(exactly = 0) {
            swapProviderTransactionsStorage.setOutgoingRecordUid(
                date = any(),
                outgoingRecordUid = any(),
            )
        }
    }

    @Test
    fun convertToViewItemCached_pendingOutgoingFallbackWithoutHash_doesNotPersistPendingUid() = runTest {
        val record = createPendingRecord(
            transactionHash = "",
        )
        val swap = createSwapProviderTransaction(outgoingRecordUid = null)

        every { swapProviderTransactionsStorage.getByOutgoingRecordUid(record.uid) } returns null
        stubOutgoingFallback(swap)

        val viewItem = factory.convertToViewItemCached(createTransactionItem(record))

        assertEquals(swap.transactionId, viewItem.changeNowTransactionId)
        verify(exactly = 0) {
            swapProviderTransactionsStorage.setOutgoingRecordUid(
                date = any(),
                outgoingRecordUid = any(),
            )
        }
    }

    private fun createUnknownSwapRecord(
        uid: String,
        valueOut: TransactionValue?,
    ): EvmTransactionRecord {
        val transaction = mockk<io.horizontalsystems.ethereumkit.models.Transaction>(relaxed = true) {
            every { hashString } returns uid
            every { transactionIndex } returns 0
            every { blockNumber } returns null
            every { timestamp } returns 1_000L
            every { isFailed } returns false
        }
        val source = mockk<TransactionSource>(relaxed = true) {
            every { blockchain } returns mockk(relaxed = true) {
                every { type } returns BlockchainType.BinanceSmartChain
            }
        }

        return EvmTransactionRecord(
            transaction = transaction,
            token = mockk<Token>(relaxed = true),
            source = source,
            protected = false,
            transactionRecordType = TransactionRecordType.EVM_UNKNOWN_SWAP,
            exchangeAddress = "0xpancakeswap_router",
            valueIn = TransactionValue.TokenValue(
                tokenName = "BNB",
                tokenCode = "BNB",
                tokenDecimals = 18,
                value = BigDecimal("-1"),
            ),
            valueOut = valueOut,
        )
    }

    private fun createPendingRecord(
        transactionHash: String,
    ): PendingTransactionRecord {
        val token = createZcashToken()

        return PendingTransactionRecord(
            uid = PENDING_UID,
            transactionHash = transactionHash,
            timestamp = PENDING_TIMESTAMP,
            source = TransactionSource(
                blockchain = token.blockchain,
                account = mockk<Account>(relaxed = true),
                meta = null,
            ),
            token = token,
            amount = ZEC_AMOUNT,
            toAddress = "t1Js8mMvZzCY2gUpTpKcNetJrMihqaPbSXF",
            fromAddress = "from-address",
            expiresAt = Long.MAX_VALUE,
            memo = null,
        )
    }

    private fun createZcashToken() = Token(
        coin = Coin(
            uid = "zcash",
            name = "Zcash",
            code = "ZEC",
        ),
        blockchain = Blockchain(
            type = BlockchainType.Zcash,
            name = "Zcash",
            eip3091url = null,
        ),
        type = TokenType.AddressSpecTyped(TokenType.AddressSpecType.Transparent),
        decimals = 8,
    )

    private fun createSwapProviderTransaction(
        outgoingRecordUid: String?,
    ) = SwapProviderTransaction(
        date = 1_000L,
        outgoingRecordUid = outgoingRecordUid,
        transactionId = "b0dd9ce8a57c7e",
        status = "new",
        provider = SwapProvider.CHANGENOW,
        coinUidIn = "zcash",
        blockchainTypeIn = BlockchainType.Zcash.uid,
        amountIn = ZEC_AMOUNT,
        addressIn = "t1Js8mMvZzCY2gUpTpKcNetJrMihqaPbSXF",
        coinUidOut = "binancecoin",
        blockchainTypeOut = BlockchainType.BinanceSmartChain.uid,
        amountOut = BigDecimal("0.01"),
        addressOut = "0xRecipient",
    )

    private fun stubOutgoingFallback(swap: SwapProviderTransaction) {
        every {
            swapProviderTransactionsStorage.getByCoinUidIn(
                coinUid = "zcash",
                blockchainType = BlockchainType.Zcash.uid,
                amountIn = ZEC_AMOUNT,
                timestamp = PENDING_TIMESTAMP * 1_000,
            )
        } returns swap
    }

    private fun createTransactionItem(record: PendingTransactionRecord) = TransactionItem(
        record = record,
        currencyValue = null,
        lastBlockInfo = null,
        nftMetadata = emptyMap(),
    )
}

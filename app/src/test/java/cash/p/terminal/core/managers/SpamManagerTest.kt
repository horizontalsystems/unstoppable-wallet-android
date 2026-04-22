package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.storage.SpamAddressStorage
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SpamManagerTest {

    private val sender = "0xabc123def456"

    private val adaptersReadyFlow = MutableStateFlow<Map<TransactionSource, ITransactionsAdapter>>(emptyMap())
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var spamManager: SpamManager

    @Before
    fun setUp() {
        val localStorage = mockk<ILocalStorage>(relaxed = true) {
            every { hideSuspiciousTransactions } returns false
        }
        val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true) {
            every { adaptersReadyFlow } returns this@SpamManagerTest.adaptersReadyFlow
        }

        spamManager = SpamManager(
            localStorage = localStorage,
            spamAddressStorage = mockk<SpamAddressStorage>(relaxed = true),
            transactionAdapterManager = transactionAdapterManager,
            dispatcherProvider = TestDispatcherProvider(testDispatcher, testScope)
        )
    }

    @After
    fun tearDown() {
        spamManager.close()
    }

    // --- subscribeToAdapters — adapter subscription lifecycle ---

    @Test
    fun subscribeToAdapters_existingAdapterOnAdditionalSourceEmission_doesNotDuplicateSubscription() = testScope.runTest {
        val firstSource = transactionSource(BlockchainType.Ethereum)
        val secondSource = transactionSource(BlockchainType.Solana)
        val firstSubscriptionCount = AtomicInteger()
        val secondSubscriptionCount = AtomicInteger()
        val firstAdapter = transactionsAdapter(subscriptionCount = firstSubscriptionCount)
        val secondAdapter = transactionsAdapter(subscriptionCount = secondSubscriptionCount)

        emitAdapters(mapOf(firstSource to firstAdapter))
        assertEquals(1, firstSubscriptionCount.get())

        emitAdapters(mapOf(firstSource to firstAdapter, secondSource to secondAdapter))

        assertEquals(1, firstSubscriptionCount.get())
        assertEquals(1, secondSubscriptionCount.get())
    }

    @Test
    fun subscribeToAdapters_removedAdapter_cancelsSubscription() = testScope.runTest {
        val source = transactionSource(BlockchainType.Ethereum)
        val cancellationCount = AtomicInteger()
        val adapter = transactionsAdapter(cancellationCount = cancellationCount)

        emitAdapters(mapOf(source to adapter))
        assertEquals(1, adapter.subscriptionCount.get())

        emitAdapters(emptyMap())

        assertEquals(1, cancellationCount.get())
    }

    // --- isSpam(events) — jetton-based spam detection ---

    @Test
    fun isSpam_zeroAmountJetton_returnsTrue() {
        val events = listOf(jettonEvent(value = BigDecimal.ZERO, symbol = "UNKNOWN"))
        assertTrue(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_zeroAmountJetton_forListedSymbol_returnsTrue() {
        val events = listOf(jettonEvent(value = BigDecimal.ZERO, symbol = "USDT"))
        assertTrue(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_nonZeroJetton_unknownSymbol_returnsFalse() {
        val events = listOf(jettonEvent(value = BigDecimal("0.00001"), symbol = "UNKNOWN"))
        assertFalse(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_nonZeroJetton_belowLimitForListedSymbol_returnsTrue() {
        val events = listOf(jettonEvent(value = BigDecimal("0.001"), symbol = "USDT"))
        assertTrue(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_nonZeroJetton_aboveLimitForListedSymbol_returnsFalse() {
        val events = listOf(jettonEvent(value = BigDecimal("1.0"), symbol = "USDT"))
        assertFalse(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_multipleEventsOneZero_returnsTrue() {
        val events = listOf(
            jettonEvent(value = BigDecimal("1.0"), symbol = "UNKNOWN"),
            jettonEvent(value = BigDecimal.ZERO, symbol = "OTHER"),
        )
        assertTrue(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_negativeZeroJetton_returnsTrue() {
        val events = listOf(jettonEvent(value = BigDecimal("-0.0"), symbol = "UNKNOWN"))
        assertTrue(SpamManager.isSpam(events))
    }

    @Test
    fun isSpam_emptyEvents_returnsFalse() {
        assertFalse(SpamManager.isSpam(emptyList()))
    }

    @Test
    fun isSpam_eventWithNullAddress_isIgnored() {
        val events = listOf(
            TransferEvent(
                address = null,
                addressForIncomingAddress = null,
                value = TransactionValue.JettonValue(
                    name = "Spam",
                    symbol = "SPM",
                    decimals = 9,
                    value = BigDecimal.ZERO,
                    image = null,
                )
            )
        )
        assertFalse(SpamManager.isSpam(events))
    }

    // --- isZeroAmountTransfer(record) — outgoing/incoming zero-value filter ---

    @Test
    fun isZeroAmountTransfer_evmOutgoingZero_returnsTrue() {
        val record = record(TransactionRecordType.EVM_OUTGOING, BigDecimal.ZERO)
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_evmIncomingZero_returnsTrue() {
        val record = record(TransactionRecordType.EVM_INCOMING, BigDecimal.ZERO)
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_bitcoinOutgoingZero_returnsTrue() {
        val record = record(TransactionRecordType.BITCOIN_OUTGOING, BigDecimal.ZERO)
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_tronIncomingZero_returnsTrue() {
        val record = record(TransactionRecordType.TRON_INCOMING, BigDecimal.ZERO)
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_stellarOutgoingZero_returnsTrue() {
        val record = record(TransactionRecordType.STELLAR_OUTGOING, BigDecimal.ZERO)
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_tonSendZero_returnsTrue() {
        val record = tonRecord(
            type = TonTransactionRecord.Action.Type.Send(
                value = jettonValue(BigDecimal.ZERO),
                to = "EQreceiver",
                sentToSelf = false,
                comment = null,
            ),
        )
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_tonReceiveZero_returnsTrue() {
        val record = tonRecord(
            type = TonTransactionRecord.Action.Type.Receive(
                value = jettonValue(BigDecimal.ZERO),
                from = "EQsender",
                to = null,
                comment = null,
            ),
        )
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_tonContractCallZero_returnsFalse() {
        // Legit TON smart-contract calls (jetton transfers, staking, NFT ops)
        // often attach 0 native TON. They must not be hidden as spam.
        val record = tonRecord(
            type = TonTransactionRecord.Action.Type.ContractCall(
                address = "EQcontract",
                value = jettonValue(BigDecimal.ZERO),
                operation = "transfer",
            ),
        )
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_tonMultipleActions_returnsFalse() {
        val record = mockk<TonTransactionRecord>()
        every { record.transactionRecordType } returns TransactionRecordType.TON
        every { record.actions } returns listOf(
            TonTransactionRecord.Action(
                type = TonTransactionRecord.Action.Type.Send(
                    value = jettonValue(BigDecimal.ZERO),
                    to = "EQa",
                    sentToSelf = false,
                    comment = null,
                ),
                status = TransactionStatus.Completed,
            ),
            TonTransactionRecord.Action(
                type = TonTransactionRecord.Action.Type.Receive(
                    value = jettonValue(BigDecimal.ZERO),
                    from = "EQb",
                    to = null,
                    comment = null,
                ),
                status = TransactionStatus.Completed,
            ),
        )
        every { record.mainValue } returns null
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_negativeZero_returnsTrue() {
        val record = record(TransactionRecordType.EVM_OUTGOING, BigDecimal("-0.0"))
        assertTrue(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_nonZero_returnsFalse() {
        val record = record(TransactionRecordType.EVM_OUTGOING, BigDecimal("0.001"))
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_evmApprove_returnsFalse() {
        val record = record(TransactionRecordType.EVM_APPROVE, BigDecimal.ZERO)
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_evmContractCall_returnsFalse() {
        val record = record(TransactionRecordType.EVM_CONTRACT_CALL, BigDecimal.ZERO)
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_evmExternalContractCall_returnsFalse() {
        val record = record(TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL, BigDecimal.ZERO)
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_swap_returnsFalse() {
        val record = record(TransactionRecordType.EVM_SWAP, BigDecimal.ZERO)
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_unknownTypeWithZero_returnsFalse() {
        val record = record(TransactionRecordType.UNKNOWN, BigDecimal.ZERO)
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    @Test
    fun isZeroAmountTransfer_nullMainValue_returnsFalse() {
        val record = mockk<TransactionRecord>()
        every { record.transactionRecordType } returns TransactionRecordType.EVM_OUTGOING
        every { record.mainValue } returns null
        assertFalse(SpamManager.isZeroAmountTransfer(record))
    }

    private fun emitAdapters(adapters: Map<TransactionSource, ITransactionsAdapter>) {
        adaptersReadyFlow.value = adapters
        testScope.advanceUntilIdle()
    }

    private fun transactionSource(blockchainType: BlockchainType): TransactionSource =
        TransactionSource(
            blockchain = mockk<Blockchain> {
                every { type } returns blockchainType
            },
            account = mockk(relaxed = true),
            meta = null
        )

    private fun transactionsAdapter(
        subscriptionCount: AtomicInteger = AtomicInteger(),
        cancellationCount: AtomicInteger = AtomicInteger()
    ): SubscriptionCountingAdapter {
        val flowable = Flowable.never<Unit>()
            .doOnSubscribe { subscriptionCount.incrementAndGet() }
            .doOnCancel { cancellationCount.incrementAndGet() }

        val adapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every { transactionsStateUpdatedFlowable } returns flowable
        }

        return SubscriptionCountingAdapter(adapter, subscriptionCount)
    }

    private data class SubscriptionCountingAdapter(
        val adapter: ITransactionsAdapter,
        val subscriptionCount: AtomicInteger
    ) : ITransactionsAdapter by adapter

    private fun jettonEvent(value: BigDecimal, symbol: String) = TransferEvent(
        address = sender,
        addressForIncomingAddress = null,
        value = TransactionValue.JettonValue(
            name = symbol,
            symbol = symbol,
            decimals = 9,
            value = value,
            image = null,
        )
    )

    private fun record(type: TransactionRecordType, amount: BigDecimal): TransactionRecord {
        val record = mockk<TransactionRecord>()
        every { record.transactionRecordType } returns type
        every { record.mainValue } returns TransactionValue.JettonValue(
            name = "TKN",
            symbol = "TKN",
            decimals = 9,
            value = amount,
            image = null,
        )
        return record
    }

    private fun jettonValue(amount: BigDecimal) = TransactionValue.JettonValue(
        name = "TKN",
        symbol = "TKN",
        decimals = 9,
        value = amount,
        image = null,
    )

    private fun tonRecord(type: TonTransactionRecord.Action.Type): TonTransactionRecord {
        val record = mockk<TonTransactionRecord>()
        every { record.transactionRecordType } returns TransactionRecordType.TON
        every { record.actions } returns listOf(
            TonTransactionRecord.Action(type = type, status = TransactionStatus.Completed)
        )
        every { record.mainValue } returns when (type) {
            is TonTransactionRecord.Action.Type.Send -> type.value
            is TonTransactionRecord.Action.Type.Receive -> type.value
            is TonTransactionRecord.Action.Type.Burn -> type.value
            is TonTransactionRecord.Action.Type.Mint -> type.value
            is TonTransactionRecord.Action.Type.ContractCall -> type.value
            else -> null
        }
        return record
    }
}

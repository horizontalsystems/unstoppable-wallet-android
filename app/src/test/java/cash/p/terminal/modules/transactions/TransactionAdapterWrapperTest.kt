package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.managers.CoinManager
import cash.p.terminal.core.managers.LocallyCreatedTransactionRepository
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionAdapterWrapperTest {
    private companion object {
        const val MWEB_ADDRESS =
            "ltcmweb1qq2nlqa567pq3hwgch23a9fhuvgfe96erem3v00gph7pjkmwrz09nkq5" +
                "fuwudw8gjmw59n6uv268r3ky23epxkr9fejdf9m6gxlkjy6lne5l82w3k"
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun get_realBitcoinRecordMatchesPendingWithoutHash_pendingIsFilteredOut() = runTest {
        val scenario = createBitcoinPendingScenario()
        val locallyCreatedTransactionRepository = mockk<LocallyCreatedTransactionRepository>(relaxed = true)
        val pendingRepository = createPendingRepository(listOf(scenario.pendingRecord))

        val wrapper = createWrapper(
            transactionWallet = scenario.transactionWallet,
            realRecords = listOf(scenario.realRecord),
            pendingRecords = listOf(scenario.pendingRecord),
            pendingRepository = pendingRepository,
            locallyCreatedTransactionRepository = locallyCreatedTransactionRepository,
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(scenario.realRecord.uid), records.map { it.uid })
        coVerify { locallyCreatedTransactionRepository.markCreated(scenario.realRecord) }
        coVerify { pendingRepository.deleteByIds(listOf(scenario.pendingRecord.uid)) }
    }

    @Test
    fun get_realRecordMatchesPendingWithoutHashButDifferentAddress_doesNotMarkRealRecordCreated() = runTest {
        val scenario = createBitcoinPendingScenario(
            realToAddress = "bc1realaddress",
            pendingToAddress = "bc1pendingaddress",
        )
        val locallyCreatedTransactionRepository = mockk<LocallyCreatedTransactionRepository>(relaxed = true)

        val wrapper = createWrapper(
            transactionWallet = scenario.transactionWallet,
            realRecords = listOf(scenario.realRecord),
            pendingRecords = listOf(scenario.pendingRecord),
            locallyCreatedTransactionRepository = locallyCreatedTransactionRepository,
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(scenario.realRecord.uid), records.map { it.uid })
        coVerify(exactly = 0) { locallyCreatedTransactionRepository.markCreated(any<TransactionRecord>()) }
    }

    @Test
    fun get_concurrentRequests_useSingleAdapterLoad() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "real-tx",
            transactionHash = "abc123",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.00000563"),
            toAddress = "bc1recipient",
        )
        var loadCount = 0
        val adapter = mockk<ITransactionsAdapter> {
            coEvery { getTransactions(any(), any(), any(), any(), any()) } coAnswers {
                loadCount++
                delay(100)
                listOf(realRecord)
            }
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
        }
        val pendingRepository = mockk<PendingTransactionRepository> {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns emptyList()
        }
        val wrapper = TransactionAdapterWrapper(
            transactionsAdapter = adapter,
            transactionWallet = transactionWallet,
            transactionType = FilterTransactionType.All,
            contact = null,
            pendingRepository = pendingRepository,
            pendingConverter = mockk(relaxed = true),
            pendingTransactionMatcher = PendingTransactionMatcher(),
            locallyCreatedTransactionRepository = mockk(relaxed = true),
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler), this),
        )

        val first = async {
            wrapper.get(
                limit = 20,
                requestedFilterType = FilterTransactionType.All,
                requestedContact = null,
            )
        }
        val second = async {
            wrapper.get(
                limit = 20,
                requestedFilterType = FilterTransactionType.All,
                requestedContact = null,
            )
        }
        advanceUntilIdle()

        assertEquals(listOf(realRecord.uid), first.await().map { it.uid })
        assertEquals(listOf(realRecord.uid), second.await().map { it.uid })
        assertEquals(1, loadCount)
    }

    @Test
    fun get_realBitcoinRecordMatchesPendingByHashWithDifferentAmount_keepsRealAmount() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "real-tx",
            transactionHash = "same-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.00000703"),
            toAddress = "bc1real-recipient",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "same-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1pending-recipient",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
        assertCoinAmount(BigDecimal("-0.00000703"), records.single())
    }

    @Test
    fun get_realBitcoinRecordDoesNotMatchPending_pendingRemainsVisible() = runTest {
        val scenario = createBitcoinPendingScenario(
            realTimestamp = 1_715_000_020,
            realAmount = BigDecimal("-0.00000703"),
        )
        val locallyCreatedTransactionRepository = mockk<LocallyCreatedTransactionRepository>(relaxed = true)

        val wrapper = createWrapper(
            transactionWallet = scenario.transactionWallet,
            realRecords = listOf(scenario.realRecord),
            pendingRecords = listOf(scenario.pendingRecord),
            locallyCreatedTransactionRepository = locallyCreatedTransactionRepository,
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(scenario.realRecord.uid, scenario.pendingRecord.uid), records.map { it.uid })
        coVerify(exactly = 0) { locallyCreatedTransactionRepository.markCreated(any<TransactionRecord>()) }
    }

    @Test
    fun get_litecoinMwebPegInPublicRecordMatchesPendingWithDifferentAmount_pendingIsFilteredOut() = runTest {
        val token = createLitecoinToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val changeAddress = "ltc1qchangeaddress"
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "public-pegin",
            transactionHash = "public-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.03509574"),
            toAddress = null,
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
            changeAddresses = listOf(changeAddress),
            toAddresses = emptyList(),
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-mweb",
            transactionHash = "mweb-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = MWEB_ADDRESS,
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
        assertCoinAmount(BigDecimal("-0.00882613"), records.single())
    }

    @Test
    fun get_litecoinMwebPegInPublicRecordWithoutRecipientsOrChange_pendingRemainsVisible() = runTest {
        val token = createLitecoinToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "ambiguous-public-send",
            transactionHash = "public-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.03509574"),
            toAddress = null,
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-mweb",
            transactionHash = "mweb-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = MWEB_ADDRESS,
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid, pendingRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_litecoinMwebPegInPublicRecordWithChangeAddress_pendingIsFilteredOut() = runTest {
        val token = createLitecoinToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val changeAddress = "ltc1qchangeaddress"
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "public-pegin",
            transactionHash = "public-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.03509574"),
            toAddress = changeAddress,
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
            changeAddresses = listOf(changeAddress),
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-mweb",
            transactionHash = "mweb-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = MWEB_ADDRESS,
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
        assertCoinAmount(BigDecimal("-0.00882613"), records.single())
    }

    @Test
    fun get_litecoinMwebPegInPublicRecordWithRecipient_pendingRemainsVisible() = runTest {
        val token = createLitecoinToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "regular-public-send",
            transactionHash = "public-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.03509574"),
            toAddress = "ltc1regularrecipient",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-mweb",
            transactionHash = "mweb-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = MWEB_ADDRESS,
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid, pendingRecord.uid), records.map { it.uid })
        assertCoinAmount(BigDecimal("-0.03509574"), records.first())
    }

    @Test
    fun get_litecoinMwebPegOutCanonicalHashWithLocalOutputId_pendingIsFilteredOut() = runTest {
        val token = createLitecoinMwebToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val localOutputId = "created-output-id"
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "mweb-outgoing:$localOutputId",
            transactionHash = "canonical-public-hash",
            timestamp = 1_715_000_600,
            amount = BigDecimal("-0.00882613"),
            toAddress = "ltc1publicdestination",
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
            showRawTransaction = false,
            canonicalTransactionHash = "canonical-public-hash",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-mweb",
            transactionHash = localOutputId,
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = "ltc1publicdestination",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_litecoinRegularPendingWithEmptyRecipientReal_pendingRemainsVisible() = runTest {
        val token = createLitecoinToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "empty-recipient-public-send",
            transactionHash = "public-hash",
            timestamp = 1_715_000_005,
            amount = BigDecimal("-0.03509574"),
            toAddress = null,
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-regular",
            transactionHash = "pending-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00882613"),
            toAddress = "ltc1regularrecipient",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid, pendingRecord.uid), records.map { it.uid })
        assertCoinAmount(BigDecimal("-0.03509574"), records.first())
    }

    @Test
    fun get_realIncomingRecordMatchesPendingByTime_pendingRemainsVisible() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "real-incoming",
            transactionHash = "hash-incoming",
            timestamp = 1_715_000_005,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1-wallet-address",
            transactionRecordType = TransactionRecordType.BITCOIN_INCOMING,
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1-recipient-address",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid, pendingRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_realOutgoingSameAddressSameAmountButOld_pendingRemainsVisible() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "real-old",
            transactionHash = "hash-old",
            timestamp = 1_700_000_000,
            amount = BigDecimal("-0.00000563"),
            toAddress = "bc1same-address",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-new",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1same-address",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(pendingRecord.uid, realRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_groupedWalletRealOutgoingDifferentToken_pendingRemainsVisible() = runTest {
        val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val source = createSource(blockchain = blockchain)
        val transactionWallet = TransactionWallet(token = null, source = source, badge = null)
        val baseToken = createEvmToken(
            coinUid = "ethereum",
            coinName = "Ethereum",
            coinCode = "ETH",
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 18,
        )
        val pendingToken = createEvmToken(
            coinUid = "usdc",
            coinName = "USD Coin",
            coinCode = "USDC",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdc"),
            decimals = 6,
        )
        val realToken = createEvmToken(
            coinUid = "usdt",
            coinName = "Tether USD",
            coinCode = "USDT",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdt"),
            decimals = 6,
        )
        startCoinManagerKoin(pendingToken)

        val realRecord = createMockRecord(
            recordUid = "real-tx",
            recordHash = "hash-real",
            txSource = source,
            recordToken = baseToken,
            txTimestamp = 1_715_000_005,
            mainValue = TransactionValue.CoinValue(realToken, BigDecimal("-12.34")),
            txToAddress = "0xrecipient",
            txType = TransactionRecordType.EVM_OUTGOING,
        )
        val pendingRecord = createPendingRecord(
            token = pendingToken,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("12.34"),
            toAddress = "0xrecipient",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid, pendingRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_groupedWalletRealOutgoingSameTokenWithBaseAsset_pendingIsFilteredOut() = runTest {
        val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val source = createSource(blockchain = blockchain)
        val transactionWallet = TransactionWallet(token = null, source = source, badge = null)
        val baseToken = createEvmToken(
            coinUid = "ethereum",
            coinName = "Ethereum",
            coinCode = "ETH",
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 18,
        )
        val pendingToken = createEvmToken(
            coinUid = "usdc",
            coinName = "USD Coin",
            coinCode = "USDC",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdc"),
            decimals = 6,
        )
        startCoinManagerKoin(pendingToken)

        val realRecord = createMockRecord(
            recordUid = "real-tx",
            recordHash = "hash-real",
            txSource = source,
            recordToken = baseToken,
            txTimestamp = 1_715_000_005,
            mainValue = TransactionValue.CoinValue(pendingToken, BigDecimal("-12.34")),
            txToAddress = "0xrecipient",
            txType = TransactionRecordType.EVM_OUTGOING,
        )
        val pendingRecord = createPendingRecord(
            token = pendingToken,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("12.34"),
            toAddress = "0xrecipient",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_groupedWalletRealSwapSameTokenWithBaseAsset_pendingIsFilteredOut() = runTest {
        val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
        val source = createSource(blockchain = blockchain)
        val transactionWallet = TransactionWallet(token = null, source = source, badge = null)
        val baseToken = createEvmToken(
            coinUid = "ethereum",
            coinName = "Ethereum",
            coinCode = "ETH",
            blockchain = blockchain,
            type = TokenType.Native,
            decimals = 18,
        )
        val pendingToken = createEvmToken(
            coinUid = "usdc",
            coinName = "USD Coin",
            coinCode = "USDC",
            blockchain = blockchain,
            type = TokenType.Eip20("0xusdc"),
            decimals = 6,
        )
        startCoinManagerKoin(pendingToken)

        val realRecord = createMockEvmSwapRecord(
            recordUid = "real-swap",
            recordHash = "hash-swap",
            txSource = source,
            recordToken = baseToken,
            txTimestamp = 1_715_000_005,
            valueIn = TransactionValue.CoinValue(pendingToken, BigDecimal("-12.34")),
            txToAddress = "0xrecipient",
        )
        val pendingRecord = createPendingRecord(
            token = pendingToken,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("12.34"),
            toAddress = "0xrecipient",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(realRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_secondPageLoad_doesNotDuplicatePendingRecord() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinRecord(
            token = token,
            source = source,
            uid = "real-tx",
            transactionHash = "real-hash",
            timestamp = 1_715_000_000,
            amount = BigDecimal("-0.00000703"),
            toAddress = "bc1-another-address",
            transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_010,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1p050tvc3q...nry6...vuvsv5t5mx",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(pendingRecord),
            realRecordPages = listOf(
                listOf(realRecord),
                emptyList(),
            ),
        )

        wrapper.get(
            limit = 1,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )
        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(pendingRecord.uid, realRecord.uid), records.map { it.uid })
    }

    @Test
    fun get_twoMatchingPendingRecordsOnlyClosestOneIsFilteredOut() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "real-tx",
            transactionHash = "real-hash",
            timestamp = 1_715_000_001,
            amount = BigDecimal("-0.00000563"),
            toAddress = "bc1same-address",
        )
        val matchedPending = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-closest",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1same-address",
        )
        val unmatchedPending = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-second",
            transactionHash = "",
            timestamp = 1_715_000_009,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1same-address",
        )

        val wrapper = createWrapper(
            transactionWallet = transactionWallet,
            realRecords = listOf(realRecord),
            pendingRecords = listOf(matchedPending, unmatchedPending),
        )

        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        assertEquals(listOf(unmatchedPending.uid, realRecord.uid), records.map { it.uid })
    }

    @Test
    fun setContact_immediatelyFollowedByGet_doesNotReturnStaleCache() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val contactA = Contact(
            uid = "contact-A",
            name = "A",
            addresses = listOf(
                ContactAddress(blockchain = token.blockchain, address = "bc1-contact-a")
            ),
        )
        val nullContactRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "null-contact-record",
            transactionHash = "hash-null",
            timestamp = 1_715_000_000,
            amount = BigDecimal("-0.00000563"),
            toAddress = "bc1-some-address",
        )
        val contactARecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "contact-a-record",
            transactionHash = "hash-a",
            timestamp = 1_715_000_010,
            amount = BigDecimal("-0.00000703"),
            toAddress = contactA.addresses.first().address,
        )

        val adapter = mockk<ITransactionsAdapter> {
            coEvery {
                getTransactions(any(), token, any(), FilterTransactionType.All, null)
            } returns listOf(nullContactRecord)
            coEvery {
                getTransactions(
                    any(), token, any(), FilterTransactionType.All, contactA.addresses.first().address
                )
            } returns listOf(contactARecord)
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
        }
        val pendingRepository = mockk<PendingTransactionRepository> {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns emptyList()
        }

        val wrapper = TransactionAdapterWrapper(
            transactionsAdapter = adapter,
            transactionWallet = transactionWallet,
            transactionType = FilterTransactionType.All,
            contact = null,
            pendingRepository = pendingRepository,
            pendingConverter = mockk(relaxed = true),
            pendingTransactionMatcher = PendingTransactionMatcher(),
            locallyCreatedTransactionRepository = mockk(relaxed = true),
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler), this),
        )

        wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = null,
        )

        wrapper.setContact(contactA)
        val records = wrapper.get(
            limit = 20,
            requestedFilterType = FilterTransactionType.All,
            requestedContact = contactA,
        )

        assertEquals(listOf(contactARecord.uid), records.map { it.uid })
        advanceUntilIdle()
    }

    private data class BitcoinPendingScenario(
        val transactionWallet: TransactionWallet,
        val realRecord: TransactionRecord,
        val pendingRecord: PendingTransactionRecord,
    )

    private fun createBitcoinPendingScenario(
        realTimestamp: Long = 1_715_000_005,
        pendingTimestamp: Long = 1_715_000_000,
        realAmount: BigDecimal = BigDecimal("-0.00000563"),
        pendingAmount: BigDecimal = BigDecimal("0.00000563"),
        realToAddress: String = "bc1p050tvc3q...nry6...vuvsv5t5mx",
        pendingToAddress: String = realToAddress,
    ): BitcoinPendingScenario {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)

        return BitcoinPendingScenario(
            transactionWallet = transactionWallet,
            realRecord = createBitcoinOutgoingRecord(
                token = token,
                source = source,
                uid = "real-tx",
                transactionHash = "abc123",
                timestamp = realTimestamp,
                amount = realAmount,
                toAddress = realToAddress,
            ),
            pendingRecord = createPendingRecord(
                token = token,
                source = source,
                uid = "pending-1",
                transactionHash = "",
                timestamp = pendingTimestamp,
                amount = pendingAmount,
                toAddress = pendingToAddress,
            ),
        )
    }

    private fun TestScope.createWrapper(
        transactionWallet: TransactionWallet,
        realRecords: List<TransactionRecord>,
        pendingRecords: List<PendingTransactionRecord>,
        realRecordPages: List<List<TransactionRecord>> = listOf(realRecords),
        pendingRepository: PendingTransactionRepository = createPendingRepository(pendingRecords),
        locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository = mockk(relaxed = true),
    ): TransactionAdapterWrapper {
        val adapterPages = realRecordPages.ifEmpty { listOf(realRecords) }
        var requestIndex = 0
        val transactionsAdapter = mockk<ITransactionsAdapter> {
            coEvery { getTransactions(any(), any(), any(), any(), any()) } coAnswers {
                val responseIndex = requestIndex.coerceAtMost(adapterPages.lastIndex)
                requestIndex++
                adapterPages[responseIndex]
            }
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
        }
        val pendingConverter = mockk<PendingTransactionConverter> {
            pendingRecords.forEach { pending ->
                every { convert(match { it.id == pending.uid }, pending.token) } returns pending
            }
        }

        return TransactionAdapterWrapper(
            transactionsAdapter = transactionsAdapter,
            transactionWallet = transactionWallet,
            transactionType = FilterTransactionType.All,
            contact = null,
            pendingRepository = pendingRepository,
            pendingConverter = pendingConverter,
            pendingTransactionMatcher = PendingTransactionMatcher(),
            locallyCreatedTransactionRepository = locallyCreatedTransactionRepository,
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler), this),
        )
    }

    private fun createPendingRepository(
        pendingRecords: List<PendingTransactionRecord>
    ): PendingTransactionRepository {
        return mockk {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns pendingRecords.map(::createPendingEntity)
            coEvery { deleteByIds(any()) } returns Unit
        }
    }

    private fun createPendingEntity(record: PendingTransactionRecord): PendingTransactionEntity {
        val token = record.token
        return PendingTransactionEntity(
            id = record.uid,
            walletId = record.source.account.id,
            coinUid = token.coin.uid,
            blockchainTypeUid = token.blockchainType.uid,
            tokenTypeId = token.type.id,
            meta = record.source.meta,
            amountAtomic = record.amount.movePointRight(token.decimals).toBigInteger().toString(),
            feeAtomic = null,
            sdkBalanceAtCreationAtomic = "0",
            fromAddress = record.from.orEmpty(),
            toAddress = record.to?.firstOrNull().orEmpty(),
            txHash = record.transactionHash.ifEmpty { null },
            nonce = null,
            memo = record.memo,
            createdAt = record.timestamp * 1000,
            expiresAt = record.expiresAt,
        )
    }

    private fun createPendingRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        transactionHash: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String,
    ) = PendingTransactionRecord(
        uid = uid,
        transactionHash = transactionHash,
        timestamp = timestamp,
        source = source,
        token = token,
        amount = amount,
        toAddress = toAddress,
        fromAddress = "",
        expiresAt = Long.MAX_VALUE,
        memo = null,
    )

    private fun createBitcoinRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        transactionHash: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String?,
        transactionRecordType: TransactionRecordType,
        changeAddresses: List<String> = emptyList(),
        toAddresses: List<String>? = toAddress?.let { listOf(it) },
        showRawTransaction: Boolean = true,
        canonicalTransactionHash: String? = null,
    ) = BitcoinTransactionRecord(
        token = token,
        amount = amount,
        to = toAddresses,
        from = null,
        changeAddresses = changeAddresses,
        uid = uid,
        transactionHash = transactionHash,
        transactionIndex = 0,
        blockHeight = null,
        confirmationsThreshold = 1,
        timestamp = timestamp,
        failed = false,
        memo = null,
        source = source,
        sentToSelf = false,
        transactionRecordType = transactionRecordType,
        fee = null,
        lockInfo = null,
        conflictingHash = null,
        showRawTransaction = showRawTransaction,
        replaceable = false,
        canonicalTransactionHash = canonicalTransactionHash,
    )

    private fun createBitcoinOutgoingRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        transactionHash: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String,
    ) = createBitcoinRecord(
        token = token,
        source = source,
        uid = uid,
        transactionHash = transactionHash,
        timestamp = timestamp,
        amount = amount,
        toAddress = toAddress,
        transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
    )

    private fun assertCoinAmount(expected: BigDecimal, record: TransactionRecord) {
        val coinValue = record.mainValue as TransactionValue.CoinValue
        assertEquals(0, expected.compareTo(coinValue.value))
    }

    private fun createToken(): Token {
        val coin = Coin(uid = "bitcoin", name = "Bitcoin", code = "BTC")
        val blockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
        return Token(
            coin = coin,
            blockchain = blockchain,
            type = TokenType.Derived(TokenType.Derivation.Bip86),
            decimals = 8,
        )
    }

    private fun createLitecoinToken(
        type: TokenType = TokenType.Derived(TokenType.Derivation.Bip84),
    ): Token {
        val coin = Coin(uid = "litecoin", name = "Litecoin", code = "LTC")
        val blockchain = Blockchain(BlockchainType.Litecoin, "Litecoin", null)
        return Token(
            coin = coin,
            blockchain = blockchain,
            type = type,
            decimals = 8,
        )
    }

    private fun createLitecoinMwebToken(): Token = createLitecoinToken(TokenType.Mweb)

    private fun createEvmToken(
        coinUid: String,
        coinName: String,
        coinCode: String,
        blockchain: Blockchain,
        type: TokenType,
        decimals: Int,
    ) = Token(
        coin = Coin(uid = coinUid, name = coinName, code = coinCode),
        blockchain = blockchain,
        type = type,
        decimals = decimals,
    )

    private fun createMockRecord(
        recordUid: String,
        recordHash: String,
        txSource: TransactionSource,
        recordToken: Token,
        txTimestamp: Long,
        mainValue: TransactionValue?,
        txToAddress: String,
        txType: TransactionRecordType,
    ): TransactionRecord = mockk {
        every { uid } returns recordUid
        every { transactionHash } returns recordHash
        every { source } returns txSource
        every { blockchainType } returns txSource.blockchain.type
        every { timestamp } returns txTimestamp
        every { token } returns recordToken
        every { to } returns listOf(txToAddress)
        every { from } returns null
        every { transactionRecordType } returns txType
        every { this@mockk.mainValue } returns mainValue
    }

    private fun createMockEvmSwapRecord(
        recordUid: String,
        recordHash: String,
        txSource: TransactionSource,
        recordToken: Token,
        txTimestamp: Long,
        valueIn: TransactionValue,
        txToAddress: String,
    ): TransactionRecord = mockk<EvmTransactionRecord> {
        every { uid } returns recordUid
        every { transactionHash } returns recordHash
        every { source } returns txSource
        every { blockchainType } returns txSource.blockchain.type
        every { timestamp } returns txTimestamp
        every { token } returns recordToken
        every { to } returns listOf(txToAddress)
        every { from } returns null
        every { transactionRecordType } returns TransactionRecordType.EVM_SWAP
        every { mainValue } returns null
        every { this@mockk.valueIn } returns valueIn
    }

    private fun startCoinManagerKoin(vararg tokens: Token) {
        val coinManager = mockk<CoinManager> {
            tokens.forEach { token ->
                every { getToken(TokenQuery(token.blockchainType, token.type)) } returns token
            }
        }

        startKoin {
            modules(module {
                single { coinManager }
            })
        }
    }

    private fun createSource(blockchain: Blockchain): TransactionSource {
        val account = Account(
            id = "account-1",
            name = "Main",
            type = mockk(relaxed = true),
            origin = AccountOrigin.Created,
            level = 0,
        )
        return TransactionSource(blockchain = blockchain, account = account, meta = null)
    }
}

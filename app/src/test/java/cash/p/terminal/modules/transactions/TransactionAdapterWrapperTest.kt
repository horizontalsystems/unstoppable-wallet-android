package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionAdapterWrapperTest {

    @Test
    fun get_realBitcoinRecordMatchesPendingWithoutHash_pendingIsFilteredOut() = runTest {
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
            toAddress = "bc1p050tvc3q...nry6...vuvsv5t5mx",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1p050tvc3q...nry6...vuvsv5t5mx",
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
    fun get_realBitcoinRecordDoesNotMatchPending_pendingRemainsVisible() = runTest {
        val token = createToken()
        val source = createSource(blockchain = token.blockchain)
        val transactionWallet = TransactionWallet(token = token, source = source, badge = null)
        val realRecord = createBitcoinOutgoingRecord(
            token = token,
            source = source,
            uid = "real-tx",
            transactionHash = "abc123",
            timestamp = 1_715_000_020,
            amount = BigDecimal("-0.00000703"),
            toAddress = "bc1p050tvc3q...nry6...vuvsv5t5mx",
        )
        val pendingRecord = createPendingRecord(
            token = token,
            source = source,
            uid = "pending-1",
            transactionHash = "",
            timestamp = 1_715_000_000,
            amount = BigDecimal("0.00000563"),
            toAddress = "bc1p050tvc3q...nry6...vuvsv5t5mx",
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

    private fun createWrapper(
        transactionWallet: TransactionWallet,
        realRecords: List<BitcoinTransactionRecord>,
        pendingRecords: List<PendingTransactionRecord>,
    ): TransactionAdapterWrapper {
        val transactionsAdapter = mockk<ITransactionsAdapter> {
            coEvery { getTransactions(any(), any(), any(), any(), any()) } returns realRecords
            every { getTransactionRecordsFlow(any(), any(), any()) } returns emptyFlow()
        }
        val pendingRepository = mockk<PendingTransactionRepository> {
            every { getActivePendingFlow(any()) } returns emptyFlow()
            coEvery { getPendingForWallet(any()) } returns pendingRecords.map(::createPendingEntity)
        }
        val pendingConverter = mockk<PendingTransactionConverter> {
            pendingRecords.forEach { pending ->
                every { convert(match { it.id == pending.uid }, transactionWallet.token!!) } returns pending
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
        )
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

    private fun createBitcoinOutgoingRecord(
        token: Token,
        source: TransactionSource,
        uid: String,
        transactionHash: String,
        timestamp: Long,
        amount: BigDecimal,
        toAddress: String,
    ) = BitcoinTransactionRecord(
        token = token,
        amount = amount,
        to = listOf(toAddress),
        from = null,
        changeAddresses = emptyList(),
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
        transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING,
        fee = null,
        lockInfo = null,
        conflictingHash = null,
        showRawTransaction = true,
        replaceable = false,
    )

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

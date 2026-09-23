package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.ISpamOutgoingContextSource
import io.horizontalsystems.walletkit.core.managers.PoisoningScorer
import io.horizontalsystems.walletkit.core.managers.SpamManager
import io.horizontalsystems.walletkit.core.managers.TransactionAdapterManager
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.core.storage.ScannedTransactionDao
import io.horizontalsystems.walletkit.core.storage.ScannedTransactionStorage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.ScannedTransaction
import io.horizontalsystems.walletkit.entities.SpamScanState
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * Drives the real SpamManager.isSpam over a real ScannedTransactionStorage to pin which verdicts
 * are cached. A stored verdict short-circuits later calls, so it must only hold an answer that
 * cannot turn out to have been wrong, not one scored against an outgoing context that never
 * loaded.
 *
 * The scenario throughout is a cent-sized USDC transfer from an address mimicking someone the
 * user really paid: +3 for dust (USDC has no micro-dust band), then +4/+4/+4 once the outgoing
 * transaction it mimics is there to correlate against.
 */
class SpamManagerCachingTest {

    // Address the user really paid, and the look-alike that mimics its prefix and suffix.
    private val realRecipient = "0xABCD567890abcdef1234567890abcdef12341234"
    private val mimic = "0xABCD999999999999999999999999999999991234"
    private val unrelated = "0x1111222233334444555566667777888899990000"

    private val blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
    private val account = Account(
        id = "acc-1",
        name = "Main",
        type = AccountType.EvmAddress(realRecipient),
        origin = AccountOrigin.Restored,
        level = 0
    )
    private val source = TransactionSource(blockchain, account, null)

    private val txHash = byteArrayOf(1, 2, 3, 4)

    private val usdc = Token(
        coin = Coin(uid = "usd-coin", name = "USD Coin", code = "USDC"),
        blockchain = blockchain,
        type = TokenType.Eip20("0xa0b8"),
        decimals = 6
    )

    private fun transfer(from: String, amount: String) =
        listOf(TransferEvent(from, TransactionValue.CoinValue(usdc, BigDecimal(amount))))

    /** USDC 0.05: under limit/10 (0.1), but USDC is in spamCoinsWithoutMicroDust, so +3, not +7. */
    private val dust = transfer(mimic, "0.05")

    private val correlatingContext = listOf(
        PoisoningScorer.OutgoingTxInfo(realRecipient, INCOMING_TIMESTAMP - 60, INCOMING_BLOCK - 1)
    )

    private val nonCorrelatingContext = listOf(
        PoisoningScorer.OutgoingTxInfo(unrelated, INCOMING_TIMESTAMP - 400_000, INCOMING_BLOCK - 30_000)
    )

    /** In-memory stand-in for the Room table, with the same read and write semantics. */
    private class FakeDao : ScannedTransactionDao {
        val rows = mutableMapOf<String, ScannedTransaction>()
        private fun key(hash: ByteArray) = hash.joinToString(",")

        override fun insert(scannedTransaction: ScannedTransaction) {
            rows[key(scannedTransaction.transactionHash)] = scannedTransaction
        }

        override fun insertAll(scannedTransactions: List<ScannedTransaction>) =
            scannedTransactions.forEach { insert(it) }

        override fun getByHash(hash: ByteArray) = rows[key(hash)]

        override fun getByHashes(hashes: List<ByteArray>) = hashes.mapNotNull { getByHash(it) }

        override fun getSpamByAddress(address: String) =
            rows.values.firstOrNull { it.isSpam && it.address.equals(address, ignoreCase = true) }

        override fun getAllSpam() = rows.values.filter { it.isSpam }

        override fun insert(spamScanState: SpamScanState) = Unit

        override fun getSpamScanState(blockchainType: BlockchainType, accountId: String): SpamScanState? = null
    }

    /**
     * Serves whatever outgoing context the test sets. An empty list is what SpamManager sees for
     * every way the context can be missing: a load that threw, an adapter not yet in adaptersMap,
     * an adapter that supplies no context, and outgoing history that has not synced yet.
     */
    private class ContextAdapter : ISpamOutgoingContextSource,
        ITransactionsAdapter by Mockito.mock(ITransactionsAdapter::class.java) {

        var context: List<PoisoningScorer.OutgoingTxInfo> = emptyList()
        var calls = 0

        override suspend fun getOutgoingContext(
            transactionHash: ByteArray,
            operationId: Long?,
            limit: Int
        ): List<PoisoningScorer.OutgoingTxInfo> {
            calls++
            return context
        }
    }

    private lateinit var dao: FakeDao
    private lateinit var adapter: ContextAdapter
    private lateinit var spamManager: SpamManager

    @Before
    fun setUp() {
        val config = Mockito.mock(IAppConfigProvider::class.java)
        Mockito.`when`(config.spamCoinValueLimits).thenReturn(mapOf("USDC" to BigDecimal("1")))
        Mockito.`when`(config.spamCoinsWithoutMicroDust).thenReturn(setOf("USDC"))
        App.appConfigProvider = config

        dao = FakeDao()
        adapter = ContextAdapter()

        val localStorage = Mockito.mock(ILocalStorage::class.java)
        Mockito.`when`(localStorage.hideSuspiciousTransactions).thenReturn(true)

        val contacts = Mockito.mock(ContactsRepository::class.java)
        Mockito.`when`(contacts.contactsFlow).thenReturn(MutableStateFlow(emptyList()))

        val adapterManager = Mockito.mock(TransactionAdapterManager::class.java)
        Mockito.`when`(adapterManager.adaptersMap).thenReturn(
            ConcurrentHashMap<TransactionSource, ITransactionsAdapter>().apply { put(source, adapter) }
        )

        spamManager = SpamManager(localStorage, ScannedTransactionStorage(dao), contacts, adapterManager)
    }

    private suspend fun classify() = spamManager.isSpam(
        transactionHash = txHash,
        events = dust,
        source = source,
        timestamp = INCOMING_TIMESTAMP,
        blockHeight = INCOMING_BLOCK,
        operationId = null
    )

    @Test
    fun `dust correlating with a recent payment is spam`() = runBlocking {
        adapter.context = correlatingContext

        assertTrue(classify())
        assertEquals(3 + 4 + 4 + 4, dao.getByHash(txHash)!!.spamScore)
    }

    @Test
    fun `verdict from a missing context is not cached and is reached again once it loads`() = runBlocking {
        adapter.context = emptyList()

        assertFalse("nothing to correlate against, so the score stays below the threshold", classify())
        assertNull("the provisional verdict must not be stored", dao.getByHash(txHash))

        adapter.context = correlatingContext

        assertTrue("once the context loads, the look-alike is caught", classify())
        assertEquals(3 + 4 + 4 + 4, dao.getByHash(txHash)!!.spamScore)
    }

    @Test
    fun `verdict scored against a real context is cached`() = runBlocking {
        adapter.context = nonCorrelatingContext

        assertFalse(classify())
        assertEquals(3, dao.getByHash(txHash)!!.spamScore)

        val callsAfterFirstPass = adapter.calls
        assertFalse(classify())
        assertEquals("the cached verdict is reused", callsAfterFirstPass, adapter.calls)
    }

    companion object {
        private const val INCOMING_TIMESTAMP = 1_700_000_000L
        private const val INCOMING_BLOCK = 18_000_000
    }
}

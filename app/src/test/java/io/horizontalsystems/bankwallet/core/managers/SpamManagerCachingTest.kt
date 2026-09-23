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
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.contacts.model.ContactAddress
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
 * cannot turn out to have been wrong: one scored against an outgoing context that never loaded,
 * or reached before the contact list - which exempts an address from scoring - was read.
 *
 * The dust scenario throughout is a cent-sized USDC transfer from an address mimicking someone the
 * user really paid: +3 for dust (USDC has no micro-dust band), then +4/+4/+4 once the outgoing
 * transaction it mimics is there to correlate against.
 */
class SpamManagerCachingTest {

    // Address the user really paid, and the look-alike that mimics its prefix and suffix.
    private val realRecipient = "0xABCD567890abcdef1234567890abcdef12341234"
    private val mimic = "0xABCD999999999999999999999999999999991234"
    private val unrelated = "0x1111222233334444555566667777888899990000"

    // Never a contact, so probing with it reflects readiness alone.
    private val probeSender = "0x7777777777777777777777777777777777777777"

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

    /** A zero-value transfer is auto-spam on its own: +7 with no correlation needed. */
    private fun zeroValueFrom(address: String) = transfer(address, "0")

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
    private lateinit var contactsFlow: MutableStateFlow<List<Contact>>
    private lateinit var contactsLoadedFlow: MutableStateFlow<Boolean>

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

        contactsFlow = MutableStateFlow(emptyList())
        contactsLoadedFlow = MutableStateFlow(false)
        val contacts = Mockito.mock(ContactsRepository::class.java)
        Mockito.`when`(contacts.contactsFlow).thenReturn(contactsFlow)
        Mockito.`when`(contacts.loadedFlow).thenReturn(contactsLoadedFlow)

        val adapterManager = Mockito.mock(TransactionAdapterManager::class.java)
        Mockito.`when`(adapterManager.adaptersMap).thenReturn(
            ConcurrentHashMap<TransactionSource, ITransactionsAdapter>().apply { put(source, adapter) }
        )

        spamManager = SpamManager(localStorage, ScannedTransactionStorage(dao), contacts, adapterManager)
    }

    private suspend fun classify(events: List<TransferEvent> = dust) = spamManager.isSpam(
        transactionHash = txHash,
        events = events,
        source = source,
        timestamp = INCOMING_TIMESTAMP,
        blockHeight = INCOMING_BLOCK,
        operationId = null
    )

    /** What ContactsRepository.initialize() does once the file read lands. */
    private fun contactsArrive(vararg contacts: Contact) {
        contactsFlow.value = contacts.toList()
        contactsLoadedFlow.value = true
        contacts.forEach { contact -> contact.addresses.forEach { awaitTrusted(it.address) } }
    }

    /**
     * Flips the loaded flag the way ContactsRepository does and waits for SpamManager to pick it
     * up: readiness is tracked against its own cache, not against the repository's flag, so
     * setting the flag alone does not mean the scoring path has seen it yet.
     */
    private fun contactsLoaded() {
        contactsLoadedFlow.value = true
        val probeHash = byteArrayOf(8, 8, 8, 8)
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            // A spam verdict is stored only once the contacts are known, so a stored probe row
            // means readiness has landed.
            runBlocking {
                spamManager.isSpam(probeHash, zeroValueFrom(probeSender), source, INCOMING_TIMESTAMP, INCOMING_BLOCK, null)
            }
            val stored = dao.getByHash(probeHash) != null
            dao.rows.remove(probeHash.joinToString(","))
            if (stored) return
            Thread.sleep(10)
        }
        throw AssertionError("the contacts-loaded flag never reached SpamManager")
    }

    /** SpamManager refreshes its trusted-address cache off its own scope, so wait for it to land. */
    private fun awaitTrusted(address: String) {
        val probeHash = byteArrayOf(9, 9, 9, 9)
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            // Zero-value transfers are auto-spam, so a not-spam verdict here means the exemption
            // has taken effect. The probe writes no row of its own once it is exempt.
            val exempt = runBlocking {
                !spamManager.isSpam(probeHash, zeroValueFrom(address), source, INCOMING_TIMESTAMP, INCOMING_BLOCK, null)
            }
            dao.rows.remove(probeHash.joinToString(","))
            if (exempt) return
            Thread.sleep(10)
        }
        throw AssertionError("trusted-address cache never picked up $address")
    }

    private fun contactFor(address: String) =
        Contact(uid = "c-1", name = "Exchange", addresses = listOf(ContactAddress(blockchain, address)))

    // ---- outgoing context --------------------------------------------------------------------

    @Test
    fun `dust correlating with a recent payment is spam`() = runBlocking {
        contactsLoaded()
        adapter.context = correlatingContext

        assertTrue(classify())
        assertEquals(3 + 4 + 4 + 4, dao.getByHash(txHash)!!.spamScore)
    }

    @Test
    fun `verdict from a missing context is not cached and is reached again once it loads`() = runBlocking {
        contactsLoaded()
        adapter.context = emptyList()

        assertFalse("nothing to correlate against, so the score stays below the threshold", classify())
        assertNull("the provisional verdict must not be stored", dao.getByHash(txHash))

        adapter.context = correlatingContext

        assertTrue("once the context loads, the look-alike is caught", classify())
        assertEquals(3 + 4 + 4 + 4, dao.getByHash(txHash)!!.spamScore)
    }

    @Test
    fun `verdict scored against a real context is cached`() = runBlocking {
        contactsLoaded()
        adapter.context = nonCorrelatingContext

        assertFalse(classify())
        assertEquals(3, dao.getByHash(txHash)!!.spamScore)

        val callsAfterFirstPass = adapter.calls
        assertFalse(classify())
        assertEquals("the cached verdict is reused", callsAfterFirstPass, adapter.calls)
    }

    // ---- contacts ----------------------------------------------------------------------------

    @Test
    fun `spam verdict reached before contacts load is not cached`() = runBlocking {
        // Contacts are read from a file well after adapters start converting transactions, so a
        // transfer from a contact can be scored while the trusted-address cache is still empty.
        val events = zeroValueFrom(unrelated)

        assertTrue("with no contacts to go on, the transfer scores as spam", classify(events))
        assertNull("but that verdict must not be stored", dao.getByHash(txHash))

        contactsArrive(contactFor(unrelated))

        assertFalse("the contact exempts it once the list is there", classify(events))
        assertNull("and no stale spam row is left behind for findSpamByAddress", dao.getByHash(txHash))
    }

    @Test
    fun `spam verdict is withheld until the contacts reach the trusted-address cache`() = runBlocking {
        val events = zeroValueFrom(unrelated)

        // ContactsRepository publishes the contacts it read and flips its loaded flag, but the
        // cache the trust check reads is filled by a collector on SpamManager's own scope, which
        // has not necessarily run yet. Whichever side of that the call lands on, the verdict is
        // not one to keep: either the exemption applies, or it could not be evaluated.
        contactsFlow.value = listOf(contactFor(unrelated))
        contactsLoadedFlow.value = true

        classify(events)
        assertNull("no spam row until the contact is in the trusted-address cache", dao.getByHash(txHash))

        awaitTrusted(unrelated)

        assertFalse(classify(events))
        assertNull(dao.getByHash(txHash))
    }

    @Test
    fun `contacts take precedence over an already stored spam verdict`() = runBlocking {
        contactsLoaded()
        val events = zeroValueFrom(unrelated)

        assertTrue(classify(events))
        assertEquals(7, dao.getByHash(txHash)!!.spamScore)

        contactsArrive(contactFor(unrelated))

        assertFalse("the stored verdict does not outlive the address becoming a contact", classify(events))
    }

    companion object {
        private const val INCOMING_TIMESTAMP = 1_700_000_000L
        private const val INCOMING_BLOCK = 18_000_000
    }
}

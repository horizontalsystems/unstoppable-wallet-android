package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference


class SpamManager(
    private val localStorage: ILocalStorage,
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val contactsRepository: ContactsRepository,
    private val transactionAdapterManager: TransactionAdapterManager
) {
    private val poisoningScorer = PoisoningScorer()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Transaction event extractors for each blockchain
    val evmExtractor = EvmTransactionEventExtractor()
    val tronExtractor = TronTransactionEventExtractor()
    val stellarExtractor = StellarTransactionEventExtractor()


    // Cache for trusted addresses from contacts (blockchainType:address -> true)
    // Key format: "blockchainTypeUid:lowercaseAddress" for fast lookup
    // Using AtomicReference to ensure consistent reads during cache updates
    private val trustedAddressesCache = AtomicReference<Set<String>>(emptySet())

    init {
        // Subscribe to contacts updates to keep cache in sync
        coroutineScope.launch {
            contactsRepository.contactsFlow.collect { contacts ->
                updateTrustedAddressesCache(contacts)
            }
        }
    }

    private fun updateTrustedAddressesCache(contacts: List<Contact>) {
        val newCache = contacts
            .flatMap { contact ->
                contact.addresses.map { addr ->
                    "${addr.blockchain.type.uid}:${addr.address.lowercase()}"
                }
            }
            .toSet()
        trustedAddressesCache.set(newCache)
    }

    private fun isAddressTrusted(address: String, blockchainType: BlockchainType): Boolean {
        val key = "${blockchainType.uid}:${address.lowercase()}"
        return trustedAddressesCache.get().contains(key)
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun findSpamByAddress(address: String): ScannedTransaction? {
        return scannedTransactionStorage.findSpamByAddress(address)
    }

    /**
     * Check if transaction is spam using a two-pass scoring approach.
     *
     * Phase 1 (fast): Value-only scoring - no DB queries for outgoing context
     * - Score >= 7: Spam (exit early)
     * - Score = 0: Not spam (exit early)
     *
     * Phase 2 (only when needed): Fetch outgoing context for correlation scoring
     * - Only called when score is in "gray zone" (1-6 points)
     *
     * Addresses in user's contacts are trusted and never flagged as spam.
     */
    suspend fun isSpam(
        transactionHash: ByteArray,
        events: List<TransferEvent>,
        source: TransactionSource,
        timestamp: Long,
        blockHeight: Int?,
        stellarOperationId: Long? = null
    ): Boolean {
        // Check database first for stored result
        scannedTransactionStorage.getScannedTransaction(transactionHash)?.let {
            return it.isSpam
        }

        val blockchainType = source.blockchain.type

        // Check if any event address is from a trusted contact - if so, not spam
        val eventAddresses = events.mapNotNull { it.address }
        if (eventAddresses.any { isAddressTrusted(it, blockchainType) }) {
            // Trusted address - save as score 0 and return not spam
            saveSpamResult(transactionHash, 0, blockchainType, null)
            return false
        }

        // No events to check = not spam
        if (events.isEmpty()) {
            saveSpamResult(transactionHash, 0, blockchainType, null)
            return false
        }

        val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits

        // Phase 1: Value-only scoring (fast, no DB calls for outgoing context)
        val valueResult = poisoningScorer.calculateValueScore(events, spamCoinLimits)

        // Early exit if score is conclusive
        if (valueResult.score >= PoisoningScorer.SPAM_THRESHOLD) {
            // Instant spam: unknown token, zero-value native coin, micro dust
            saveSpamResult(transactionHash, valueResult.score, blockchainType, valueResult.address)
            return true
        }

        if (valueResult.score == 0) {
            // Not spam: normal value transfer
            saveSpamResult(transactionHash, 0, blockchainType, null)
            return false
        }

        // Phase 2: Score is in gray zone (1-6), need correlation check
        // Only now do we fetch outgoing context (expensive DB queries)
        val outgoingContext = getOutgoingContext(source, transactionHash, stellarOperationId)
        val correlationResult = poisoningScorer.calculateCorrelationScore(
            events = events,
            incomingTimestamp = timestamp,
            incomingBlockHeight = blockHeight,
            recentOutgoingTxs = outgoingContext
        )

        // Final score = value score + correlation score
        val finalScore = valueResult.score + correlationResult.points
        val spamAddress = valueResult.address ?: correlationResult.address

        saveSpamResult(transactionHash, finalScore, blockchainType, spamAddress)

        return finalScore >= PoisoningScorer.SPAM_THRESHOLD
    }

    /**
     * Get recent outgoing transactions for correlation context.
     */
    private suspend fun getOutgoingContext(
        source: TransactionSource,
        transactionHash: ByteArray,
        stellarOperationId: Long?,
    ): List<PoisoningScorer.OutgoingTxInfo> {
        val adapter = transactionAdapterManager.adaptersMap[source] ?: return emptyList()

        return try {
            when (adapter) {
                is EvmTransactionsAdapter -> {
                    val userAddress = adapter.evmKitWrapper.evmKit.receiveAddress
                    adapter.getFullTransactionsBefore(transactionHash, OUTGOING_CONTEXT_SIZE)
                        .sortedByDescending { it.transaction.timestamp }
                        .mapNotNull { evmExtractor.extractOutgoingInfo(it, userAddress) }
                }
                is TronTransactionsAdapter -> {
                    val userAddress = adapter.tronKitWrapper.tronKit.address
                    adapter.getTronFullTransactionsBefore(transactionHash, OUTGOING_CONTEXT_SIZE)
                        .sortedByDescending { it.transaction.timestamp }
                        .mapNotNull { tronExtractor.extractOutgoingInfo(it, userAddress) }
                }
                is StellarTransactionsAdapter -> {
                    val selfAddress = adapter.stellarKitWrapper.stellarKit.receiveAddress
                    adapter.getStellarOperationsBefore(stellarOperationId, OUTGOING_CONTEXT_SIZE)
                        .sortedByDescending { it.timestamp }
                        .mapNotNull { stellarExtractor.extractOutgoingInfo(it, selfAddress) }
                }
                else -> emptyList()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting outgoing context", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "SpamManager"
        private const val OUTGOING_CONTEXT_SIZE = 20
    }

    // ==================== Common Processing Logic ====================

    /**
     * Save spam detection result to database.
     */
    private fun saveSpamResult(
        transactionHash: ByteArray,
        spamScore: Int,
        blockchainType: BlockchainType,
        spamAddress: String?
    ) {
        try {
            scannedTransactionStorage.save(
                ScannedTransaction(
                    transactionHash = transactionHash,
                    spamScore = spamScore,
                    blockchainType = blockchainType,
                    address = spamAddress
                )
            )
        } catch (_: Throwable) {
        }
    }

}
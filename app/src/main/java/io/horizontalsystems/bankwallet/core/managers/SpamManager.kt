package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private fun isEvmBlockchain(source: TransactionSource): Boolean {
    return EvmBlockchainManager.blockchainTypes.contains(source.blockchain.type)
}

class SpamManager(
    private val localStorage: ILocalStorage,
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val contactsRepository: ContactsRepository,
    private val transactionAdapterManager: TransactionAdapterManager
) {
    private val poisoningScorer = PoisoningScorer()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Lazy initialized SpamRescanManager to avoid circular dependency
    private val spamRescanManager: SpamRescanManager by lazy {
        SpamRescanManager(scannedTransactionStorage, this)
    }

    // Cache for recent outgoing transactions per token (tokenUid -> list of outgoing tx info)
    // Used for address poisoning detection - populated when outgoing transactions are processed
    private val outgoingTxCache = mutableMapOf<String, MutableList<PoisoningScorer.OutgoingTxInfo>>()
    private val cacheLock = Any()
    private val initializedSources = mutableSetOf<TransactionSource>()

    // Cache for trusted addresses from contacts (blockchainType:address -> true)
    // Key format: "blockchainTypeUid:lowercaseAddress" for fast lookup
    @Volatile
    private var trustedAddressesCache: Set<String> = emptySet()

    companion object {
        private const val CACHE_SIZE_PER_TOKEN = 10
        private const val INITIAL_CACHE_SIZE = 10
    }

    init {
        // Subscribe to contacts updates to keep cache in sync
        coroutineScope.launch {
            contactsRepository.contactsFlow.collect { contacts ->
                updateTrustedAddressesCache(contacts)
            }
        }
    }

    private fun updateTrustedAddressesCache(contacts: List<Contact>) {
        trustedAddressesCache = contacts
            .flatMap { contact ->
                contact.addresses.map { addr ->
                    "${addr.blockchain.type.uid}:${addr.address.lowercase()}"
                }
            }
            .toSet()
    }

    private fun isAddressTrusted(address: String, blockchainType: BlockchainType): Boolean {
        val key = "${blockchainType.uid}:${address.lowercase()}"
        return trustedAddressesCache.contains(key)
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    /**
     * Initialize cache with recent outgoing transactions for EVM sources.
     * Called once per source on first run to pre-populate the cache.
     */
    fun initializeCache(transactionAdapterManager: TransactionAdapterManager) {
        coroutineScope.launch {
            transactionAdapterManager.adaptersMap.forEach { (source, adapter) ->
                if (isEvmBlockchain(source) && !initializedSources.contains(source)) {
                    initializeCacheForSource(source, adapter)
                    initializedSources.add(source)
                }
            }
        }
    }

    private suspend fun initializeCacheForSource(source: TransactionSource, adapter: ITransactionsAdapter) {
        try {
            val outgoingTxs = adapter.getTransactions(
                from = null,
                token = null,
                limit = INITIAL_CACHE_SIZE,
                transactionType = FilterTransactionType.Outgoing,
                address = null
            )

            outgoingTxs.forEach { tx ->
                extractOutgoingTxInfo(tx, source)?.let { (tokenUid, txInfo) ->
                    synchronized(cacheLock) {
                        val txList = outgoingTxCache.getOrPut(tokenUid) { mutableListOf() }
                        if (txList.size < CACHE_SIZE_PER_TOKEN) {
                            txList.add(txInfo)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore errors during initialization
        }
    }

    private fun extractOutgoingTxInfo(
        tx: TransactionRecord,
        source: TransactionSource
    ): Pair<String, PoisoningScorer.OutgoingTxInfo>? {
        return when (tx) {
            is EvmOutgoingTransactionRecord -> {
                val tokenUid = "${source.blockchain.type.uid}:native"
                val txInfo = PoisoningScorer.OutgoingTxInfo(tx.to, tx.timestamp, tx.blockHeight)
                tokenUid to txInfo
            }
            is ContractCallTransactionRecord -> {
                tx.outgoingEvents.firstOrNull()?.let { event ->
                    val tokenUid = "${source.blockchain.type.uid}:native"
                    event.address?.let { address ->
                        val txInfo = PoisoningScorer.OutgoingTxInfo(address, tx.timestamp, tx.blockHeight)
                        tokenUid to txInfo
                    }
                }
            }
            else -> null
        }
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun findSpamByAddress(address: String): ScannedTransaction? {
        return scannedTransactionStorage.findSpamByAddress(address)
    }

    /**
     * Add outgoing transaction to cache for a token.
     * Called when processing outgoing transactions to build context for spam detection.
     */
    fun addOutgoingTransaction(tokenUid: String, recipientAddress: String, timestamp: Long, blockHeight: Int?) {
        synchronized(cacheLock) {
            val txList = outgoingTxCache.getOrPut(tokenUid) { mutableListOf() }
            val txInfo = PoisoningScorer.OutgoingTxInfo(recipientAddress, timestamp, blockHeight)

            // Add at the beginning (most recent first)
            txList.add(0, txInfo)

            // Keep only the most recent transactions
            if (txList.size > CACHE_SIZE_PER_TOKEN) {
                txList.removeAt(txList.size - 1)
            }
        }
    }

    /**
     * Get cached outgoing transactions for a token.
     * Returns only cached data - does not fetch from adapter to avoid memory pressure.
     * Cache is populated when outgoing transactions are processed via addOutgoingTransaction().
     */
    private fun getOutgoingTxsFromCache(tokenUid: String): List<PoisoningScorer.OutgoingTxInfo> {
        synchronized(cacheLock) {
            return outgoingTxCache[tokenUid]?.toList() ?: emptyList()
        }
    }

    /**
     * Check if transaction is spam.
     * First checks stored result in database. If not found, triggers background scan
     * and waits for result to ensure accurate spam detection with full outgoing context.
     * Addresses in user's contacts are trusted and never flagged as spam.
     *
     * @param transactionHash Transaction hash for database lookup
     * @param events List of transfer events to check (only used if not in database)
     * @param timestamp Transaction timestamp
     * @param blockHeight Transaction block height (nullable)
     * @param tokenUid Token identifier for cache lookup
     * @param source Transaction source
     */
    fun isSpam(
        transactionHash: ByteArray,
        events: List<TransferEvent>,
        timestamp: Long,
        blockHeight: Int?,
        tokenUid: String?,
        source: TransactionSource
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
            try {
                scannedTransactionStorage.save(
                    ScannedTransaction(
                        transactionHash = transactionHash,
                        spamScore = 0,
                        blockchainType = blockchainType,
                        address = null
                    )
                )
            } catch (_: Throwable) {
            }
            return false
        }

        // Not in database - trigger on-demand scan and wait for result
        // This ensures proper outgoing context is built for accurate spam detection
        if (isEvmBlockchain(source)) {
            val adapter = transactionAdapterManager.adaptersMap[source]
            if (adapter != null) {
                runBlocking {
                    spamRescanManager.ensureTransactionScanned(transactionHash, source, adapter)
                }

                // Check database again after scan
                scannedTransactionStorage.getScannedTransaction(transactionHash)?.let {
                    return it.isSpam
                }
            }
        }

        // Fallback: calculate with available context (non-EVM or adapter not found)
        val recentOutgoingTxs = tokenUid?.let { getOutgoingTxsFromCache(it) } ?: emptyList()

        val scoringResult = poisoningScorer.calculateSpamScore(
            events = events,
            incomingTimestamp = timestamp,
            incomingBlockHeight = blockHeight,
            recentOutgoingTxs = recentOutgoingTxs
        )

        return scoringResult.score >= PoisoningScorer.SPAM_THRESHOLD
    }

    /**
     * Extract outgoing transaction info directly from FullTransaction.
     * Works with decoration types instead of TransactionRecord.
     * Used by SpamRescanManager to build context for spam detection without triggering isSpam().
     */
    fun extractOutgoingInfoFromFullTransaction(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is OutgoingDecoration -> {
                PoisoningScorer.OutgoingTxInfo(
                    decoration.to.eip55,
                    tx.timestamp,
                    tx.blockNumber?.toInt()
                )
            }
            is OutgoingEip20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(
                    decoration.to.eip55,
                    tx.timestamp,
                    tx.blockNumber?.toInt()
                )
            }
            is UnknownTransactionDecoration -> {
                // Only extract outgoing info if user initiated the transaction
                if (tx.from == userAddress) {
                    val outgoingTransfers = decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .filter { it.from == userAddress }
                    outgoingTransfers.firstOrNull()?.let { transfer ->
                        PoisoningScorer.OutgoingTxInfo(
                            transfer.to.eip55,
                            tx.timestamp,
                            tx.blockNumber?.toInt()
                        )
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * Process a FullTransaction for spam detection during background rescan.
     * Works directly with FullTransaction to avoid triggering isSpam() during conversion.
     *
     * @param fullTx Full transaction to process
     * @param source Transaction source
     * @param userAddress User's wallet address
     * @param baseToken Base token for native value conversion
     * @param outgoingContext List of recent outgoing transactions for poisoning detection
     */
    fun processFullTransactionForSpamWithContext(
        fullTx: FullTransaction,
        source: TransactionSource,
        userAddress: Address,
        baseToken: Token,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        val tx = fullTx.transaction
        val txHashBytes = tx.hash

        // Skip if already scanned
        scannedTransactionStorage.getScannedTransaction(txHashBytes)?.let { return }

        // Extract incoming events based on decoration type
        val incomingEvents: List<TransferEvent> = when (val decoration = fullTx.decoration) {
            is IncomingDecoration -> {
                val value = decoration.value.toBigDecimal(baseToken.decimals)
                listOf(TransferEvent(decoration.from.eip55, TransactionValue.CoinValue(baseToken, value)))
            }
            is UnknownTransactionDecoration -> {
                // Only process if not user-initiated (external contract call)
                if (tx.from != userAddress) {
                    val incomingTransfers = decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                    incomingTransfers.map { transfer ->
                        val tokenValue = transfer.tokenInfo?.let { info ->
                            TransactionValue.TokenValue(
                                tokenName = info.tokenName,
                                tokenCode = info.tokenSymbol,
                                tokenDecimals = info.tokenDecimal,
                                value = transfer.value.toBigDecimal(info.tokenDecimal),
                            )
                        } ?: TransactionValue.RawValue(transfer.value)
                        // Use transfer.from (sender address) for mimic detection
                        TransferEvent(transfer.to.eip55, tokenValue)
                    }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }

        // Calculate spam score - only transactions with incoming events can be spam
        val scoringResult = if (incomingEvents.isEmpty()) {
            // No incoming events = score 0 (outgoing, contract calls, etc.)
            PoisoningScorer.SpamScoringResult(0, null)
        } else {
            // Check if any event address is from a trusted contact
            val eventAddresses = incomingEvents.mapNotNull { it.address }
            val blockchainType = source.blockchain.type
            if (eventAddresses.any { isAddressTrusted(it, blockchainType) }) {
                // Trusted address - score 0
                PoisoningScorer.SpamScoringResult(0, null)
            } else {
                poisoningScorer.calculateSpamScore(
                    events = incomingEvents,
                    incomingTimestamp = tx.timestamp,
                    incomingBlockHeight = tx.blockNumber?.toInt(),
                    recentOutgoingTxs = outgoingContext
                )
            }
        }

        // Save result to database for all transactions
        try {
            scannedTransactionStorage.save(
                ScannedTransaction(
                    transactionHash = txHashBytes,
                    spamScore = scoringResult.score,
                    blockchainType = source.blockchain.type,
                    address = scoringResult.spamAddress
                )
            )
        } catch (_: Throwable) {
        }
    }
}
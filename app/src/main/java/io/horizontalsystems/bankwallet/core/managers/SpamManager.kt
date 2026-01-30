package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

private fun isEvmBlockchain(source: TransactionSource): Boolean {
    return EvmBlockchainManager.blockchainTypes.contains(source.blockchain.type)
}

class SpamManager(
    private val localStorage: ILocalStorage,
    private val scannedTransactionStorage: ScannedTransactionStorage
) {
    private val poisoningScorer = PoisoningScorer()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Cache for recent outgoing transactions per token (tokenUid -> list of outgoing tx info)
    // Used for address poisoning detection - populated when outgoing transactions are processed
    private val outgoingTxCache = mutableMapOf<String, MutableList<PoisoningScorer.OutgoingTxInfo>>()
    private val cacheLock = Any()
    private val initializedSources = mutableSetOf<TransactionSource>()

    companion object {
        private const val CACHE_SIZE_PER_TOKEN = 10
        private const val INITIAL_CACHE_SIZE = 10
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
     * First checks stored result in database, then calculates if not found.
     *
     * @param transactionHash Transaction hash for database lookup
     * @param events List of transfer events to check (only used if not in database)
     * @param timestamp Transaction timestamp
     * @param blockHeight Transaction block height (nullable)
     * @param tokenUid Token identifier for cache lookup
     * @param source Transaction source
     */
    suspend fun isSpam(
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

        // Not in database, calculate spam status
        val dustSpamAddresses = handleSpamAddresses(events)

        val recentOutgoingTxs = tokenUid?.let { getOutgoingTxsFromCache(it) } ?: emptyList()

        val poisoningSpamAddresses = poisoningScorer.detectSpamAddresses(
            events = events,
            incomingTimestamp = timestamp,
            incomingBlockHeight = blockHeight,
            recentOutgoingTxs = recentOutgoingTxs
        )

        val spamAddresses = (dustSpamAddresses + poisoningSpamAddresses).distinct()
        val isSpam = spamAddresses.isNotEmpty()

        // Save result to database
        try {
            scannedTransactionStorage.save(
                ScannedTransaction(
                    transactionHash = transactionHash,
                    isSpam = isSpam,
                    blockchainType = source.blockchain.type,
                    address = spamAddresses.firstOrNull()
                )
            )
        } catch (_: Throwable) {
        }

        return isSpam
    }

    private fun handleSpamAddresses(events: List<TransferEvent>): List<String> {
        val spamTokenSenders = mutableListOf<String>()
        val nativeSenders = mutableListOf<String>()
        var totalNativeTransactionValue: TransactionValue? = null

        events.forEach { event ->
            if (event.value is TransactionValue.CoinValue && event.value.token.type == TokenType.Native) {
                val totalNativeValue = totalNativeTransactionValue?.decimalValue ?: BigDecimal.ZERO
                totalNativeTransactionValue = TransactionValue.CoinValue(event.value.token, event.value.value + totalNativeValue)
                event.address?.let { nativeSenders.add(it) }
            } else {
                if (event.address != null && isSpamValue(event.value)) {
                    spamTokenSenders.add(event.address)
                }
            }
        }

        totalNativeTransactionValue?.let { nativeValue ->
            if (isSpamValue(nativeValue) && nativeSenders.isNotEmpty()) {
                spamTokenSenders.addAll(nativeSenders)
            }
        }

        return spamTokenSenders
    }

    private fun isSpamValue(transactionValue: TransactionValue): Boolean {
        val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits
        val value = transactionValue.decimalValue

        var limit: BigDecimal = BigDecimal.ZERO
        when (transactionValue) {
            is TransactionValue.CoinValue -> {
                limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
            }

            is TransactionValue.JettonValue -> {
                limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
            }

            is TransactionValue.NftValue -> {
                if (transactionValue.value > BigDecimal.ZERO)
                    return false
            }

            is TransactionValue.RawValue,
            is TransactionValue.TokenValue -> {
                return true
            }
        }

        return limit > value
    }

    /**
     * Process a transaction for spam detection during background rescan.
     * Uses explicit outgoing transaction context instead of cache.
     *
     * @param tx Transaction record to process
     * @param source Transaction source
     * @param outgoingContext List of recent outgoing transactions for poisoning detection
     */
    suspend fun processTransactionForSpamWithContext(
        tx: TransactionRecord,
        source: TransactionSource,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        val txHashBytes = tx.transactionHash.hexStringToByteArray()

        // Skip if already scanned
        scannedTransactionStorage.getScannedTransaction(txHashBytes)?.let { return }

        val events: List<TransferEvent> = when (tx) {
            is EvmIncomingTransactionRecord -> listOf(TransferEvent(tx.from, tx.value))
            is ExternalContractCallTransactionRecord -> tx.incomingEvents + tx.outgoingEvents
            is ContractCallTransactionRecord -> tx.incomingEvents + tx.outgoingEvents
            else -> return // Skip other transaction types
        }

        if (events.isEmpty()) return

        // Calculate spam status with explicit context
        val dustSpamAddresses = handleSpamAddresses(events)

        val poisoningSpamAddresses = poisoningScorer.detectSpamAddresses(
            events = events,
            incomingTimestamp = tx.timestamp,
            incomingBlockHeight = tx.blockHeight,
            recentOutgoingTxs = outgoingContext
        )

        val spamAddresses = (dustSpamAddresses + poisoningSpamAddresses).distinct()
        val isSpam = spamAddresses.isNotEmpty()

        // Save result to database
        try {
            scannedTransactionStorage.save(
                ScannedTransaction(
                    transactionHash = txHashBytes,
                    isSpam = isSpam,
                    blockchainType = source.blockchain.type,
                    address = spamAddresses.firstOrNull()
                )
            )
        } catch (_: Throwable) {
        }
    }

    /**
     * Extract outgoing transaction info from a transaction record.
     * Used by SpamRescanManager to build context for spam detection.
     */
    fun extractOutgoingInfo(tx: TransactionRecord): PoisoningScorer.OutgoingTxInfo? {
        return when (tx) {
            is EvmOutgoingTransactionRecord -> {
                PoisoningScorer.OutgoingTxInfo(tx.to, tx.timestamp, tx.blockHeight)
            }
            is ContractCallTransactionRecord -> {
                tx.outgoingEvents.firstOrNull()?.address?.let { address ->
                    PoisoningScorer.OutgoingTxInfo(address, tx.timestamp, tx.blockHeight)
                }
            }
            is ExternalContractCallTransactionRecord -> {
                tx.outgoingEvents.firstOrNull()?.address?.let { address ->
                    PoisoningScorer.OutgoingTxInfo(address, tx.timestamp, tx.blockHeight)
                }
            }
            else -> null
        }
    }
}

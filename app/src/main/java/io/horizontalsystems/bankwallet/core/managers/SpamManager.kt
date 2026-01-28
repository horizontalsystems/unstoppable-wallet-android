package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

class SpamManager(
    private val localStorage: ILocalStorage,
    private val scannedTransactionStorage: ScannedTransactionStorage
) {
    private var transactionAdapterManager: TransactionAdapterManager? = null
    private val poisoningScorer = PoisoningScorer()

    // Cache for recent outgoing transactions per token (tokenUid -> list of outgoing tx info)
    // Used for address poisoning detection without database calls
    private val outgoingTxCache = mutableMapOf<String, MutableList<PoisoningScorer.OutgoingTxInfo>>()
    private val cacheLock = Any()

    companion object {
        private const val CACHE_SIZE_PER_TOKEN = 10
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun set(transactionAdapterManager: TransactionAdapterManager) {
        this.transactionAdapterManager = transactionAdapterManager
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun findSpamByAddress(address: String): ScannedTransaction? {
        return scannedTransactionStorage.findSpamByAddress(address)
    }

    /**
     * Fetch recent outgoing transactions for address poisoning comparison.
     */
    private suspend fun fetchRecentOutgoingTransactions(
        adapter: ITransactionsAdapter,
        limit: Int
    ): List<PoisoningScorer.OutgoingTxInfo> {
        return try {
            val outgoingTxs = adapter.getTransactions(
                from = null,
                token = null,
                limit = limit,
                transactionType = FilterTransactionType.Outgoing,
                address = null
            )

            outgoingTxs.mapNotNull { tx ->
                extractOutgoingTxInfo(tx)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Extract recipient address and timing info from outgoing transaction records.
     */
    private fun extractOutgoingTxInfo(tx: TransactionRecord): PoisoningScorer.OutgoingTxInfo? {
        val recipientAddress = when (tx) {
            is EvmOutgoingTransactionRecord -> tx.to
            is TronOutgoingTransactionRecord -> tx.to
            is ContractCallTransactionRecord -> {
                // For contract calls, get the first outgoing event recipient
                tx.outgoingEvents.firstOrNull()?.address
            }
            is TronContractCallTransactionRecord -> {
                tx.outgoingEvents.firstOrNull()?.address
            }
            else -> null
        }

        return recipientAddress?.let {
            PoisoningScorer.OutgoingTxInfo(
                recipientAddress = it,
                timestamp = tx.timestamp,
                blockHeight = tx.blockHeight
            )
        }
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
     * If cache is empty, fetches from database and populates cache.
     */
    private suspend fun getOrFetchOutgoingTxs(
        tokenUid: String,
        source: TransactionSource
    ): List<PoisoningScorer.OutgoingTxInfo> {
        // Check cache first
        synchronized(cacheLock) {
            outgoingTxCache[tokenUid]?.let { cached ->
                if (cached.isNotEmpty()) {
                    return cached.toList()
                }
            }
        }

        // Cache is empty, fetch from database
        val adapter = transactionAdapterManager?.getAdapter(source) ?: return emptyList()
        val fetchedTxs = fetchRecentOutgoingTransactions(adapter, CACHE_SIZE_PER_TOKEN)

        // Populate cache with fetched transactions
        synchronized(cacheLock) {
            val txList = outgoingTxCache.getOrPut(tokenUid) { mutableListOf() }
            txList.clear()
            txList.addAll(fetchedTxs)
        }

        return fetchedTxs
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

        val recentOutgoingTxs = tokenUid?.let { getOrFetchOutgoingTxs(it, source) } ?: emptyList()

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
}

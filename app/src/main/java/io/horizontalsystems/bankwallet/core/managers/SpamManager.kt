package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.factories.TransferEventFactory
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.util.concurrent.Executors

class SpamManager(
    private val localStorage: ILocalStorage,
    private val spamAddressStorage: SpamAddressStorage
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private var transactionAdapterManager: TransactionAdapterManager? = null
    private val transferEventFactory = TransferEventFactory()
    private val poisoningScorer = PoisoningScorer()

    // Cache for recent outgoing transactions per token (tokenUid -> list of outgoing tx info)
    // Used for address poisoning detection without database calls
    private val outgoingTxCache = mutableMapOf<String, MutableList<PoisoningScorer.OutgoingTxInfo>>()
    private val cacheLock = Any()

    companion object {
        private const val OUTGOING_TX_LIMIT = 15
        private const val CACHE_SIZE_PER_TOKEN = 10
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun set(transactionAdapterManager: TransactionAdapterManager) {
        this.transactionAdapterManager = transactionAdapterManager

        coroutineScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect {
                subscribeToAdapters(transactionAdapterManager)
            }
        }
    }

    private fun subscribeToAdapters(transactionAdapterManager: TransactionAdapterManager) {
        transactionAdapterManager.adaptersMap.forEach { (transactionSource, transactionsAdapter) ->
            subscribeToAdapter(transactionSource, transactionsAdapter)
        }
    }

    private fun subscribeToAdapter(source: TransactionSource, adapter: ITransactionsAdapter) {
        coroutineScope.launch {
            adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                sync(source)
            }
        }
    }

    private fun sync(source: TransactionSource) {
        singleDispatcherCoroutineScope.launch {
            val adapter = transactionAdapterManager?.getAdapter(source) ?: run {
                return@launch
            }
            val spamScanState = spamAddressStorage.getSpamScanState(source.blockchain.type, source.account.id)
            val transactions = adapter.getTransactionsAfter(spamScanState?.lastSyncedTransactionId)

            // Fetch recent outgoing transactions for address poisoning detection
            val recentOutgoingTxs = fetchRecentOutgoingTransactions(adapter, OUTGOING_TX_LIMIT)

            val lastSyncedTransactionId = handle(transactions, source, recentOutgoingTxs)
            lastSyncedTransactionId?.let {
                spamAddressStorage.save(SpamScanState(source.blockchain.type, source.account.id, lastSyncedTransactionId))
            }
        }
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
        } catch (e: Throwable) {
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

    private fun handle(
        transactions: List<TransactionRecord>,
        source: TransactionSource,
        recentOutgoingTxs: List<PoisoningScorer.OutgoingTxInfo>
    ): String? {
        val spamAddresses = mutableListOf<SpamAddress>()

        transactions.forEach { transaction ->
            val hashByteArray = transaction.transactionHash.hexStringToByteArrayOrNull() ?: return@forEach
            val events = transferEventFactory.transferEvents(transaction)
            if (events.isEmpty()) return@forEach

            // Use enhanced detection with poisoning scoring
            val detectedSpamAddresses = detectSpamAddressesWithScoring(
                events = events,
                incomingTimestamp = transaction.timestamp,
                incomingBlockHeight = transaction.blockHeight,
                recentOutgoingTxs = recentOutgoingTxs
            )

            detectedSpamAddresses.forEach { address ->
                spamAddresses.add(SpamAddress(hashByteArray, address, null, source.blockchain.type))
            }
        }

        try {
            spamAddressStorage.save(spamAddresses)
        } catch (_: Throwable) {
        }

        val sortedTransactions = transactions.sortedWith(
            compareBy<TransactionRecord> { it.timestamp }
                .thenBy { it.transactionIndex }
                .thenBy { it.transactionHash }
        )

        return sortedTransactions.lastOrNull()?.transactionHash
    }

    /**
     * Enhanced spam detection combining dust detection and address poisoning scoring.
     */
    private fun detectSpamAddressesWithScoring(
        events: List<TransferEvent>,
        incomingTimestamp: Long,
        incomingBlockHeight: Int?,
        recentOutgoingTxs: List<PoisoningScorer.OutgoingTxInfo>
    ): List<String> {
        // First, use the original dust-based detection
        val dustSpamAddresses = handleSpamAddresses(events)

        // Then, use the poisoning scorer for enhanced detection
        val poisoningSpamAddresses = if (recentOutgoingTxs.isNotEmpty()) {
            poisoningScorer.detectSpamAddresses(
                events = events,
                incomingTimestamp = incomingTimestamp,
                incomingBlockHeight = incomingBlockHeight,
                recentOutgoingTxs = recentOutgoingTxs
            )
        } else {
            emptyList()
        }

        // Combine results
        return (dustSpamAddresses + poisoningSpamAddresses).distinct()
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun find(address: String): SpamAddress? {
        return spamAddressStorage.findByAddress(address)
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
     * Check if events represent spam transactions.
     * Uses cached outgoing transactions for address poisoning detection.
     * If cache is empty, fetches from database automatically.
     *
     * @param events List of transfer events to check
     * @param timestamp Transaction timestamp
     * @param blockHeight Transaction block height (nullable)
     * @param tokenUid Token identifier for cache lookup (e.g., "ethereum:native" or "ethereum:0x...")
     * @param source Transaction source for fetching from database when cache is empty
     */
    suspend fun isSpam(
        events: List<TransferEvent>,
        timestamp: Long,
        blockHeight: Int?,
        tokenUid: String?,
        source: TransactionSource
    ): Boolean {
        // First, use the original dust-based detection
        val dustSpamAddresses = handleSpamAddresses(events)

        // Get outgoing transactions (from cache or fetch from database)
        val recentOutgoingTxs = tokenUid?.let { getOrFetchOutgoingTxs(it, source) } ?: emptyList()

        // Use PoisoningScorer with outgoing transactions
        val poisoningSpamAddresses = poisoningScorer.detectSpamAddresses(
            events = events,
            incomingTimestamp = timestamp,
            incomingBlockHeight = blockHeight,
            recentOutgoingTxs = recentOutgoingTxs
        )

        return (dustSpamAddresses + poisoningSpamAddresses).isNotEmpty()
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

        if (totalNativeTransactionValue != null && isSpamValue(totalNativeTransactionValue!!) && nativeSenders.isNotEmpty()) {
            spamTokenSenders.addAll(nativeSenders)
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

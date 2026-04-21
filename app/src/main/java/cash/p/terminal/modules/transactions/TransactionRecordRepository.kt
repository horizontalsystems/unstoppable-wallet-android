package cash.p.terminal.modules.transactions

import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.getShortOutgoingTransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: TransactionAdapterManager,
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage,
    private val pendingRepository: PendingTransactionRepository,
    private val pendingConverter: PendingTransactionConverter,
    private val pendingTransactionMatcher: PendingTransactionMatcher,
    private val dispatcherProvider: DispatcherProvider
) : ITransactionRecordRepository {

    @Volatile
    private var selectedFilterTransactionType: FilterTransactionType = FilterTransactionType.All

    private var selectedWallet: TransactionWallet? = null
    private var selectedBlockchain: Blockchain? = null
    private var contact: Contact? = null

    private val _itemsFlow = MutableSharedFlow<List<TransactionRecord>>(extraBufferCapacity = 4)
    override val itemsFlow: SharedFlow<List<TransactionRecord>> = _itemsFlow.asSharedFlow()

    @Volatile
    private var loadedPageNumber = 0

    private var allNormalLoaded = AtomicBoolean(false)
    private var allExtraLoaded = AtomicBoolean(false)

    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()
    private val extraSwapAdaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val coroutineScope = CoroutineScope(dispatcherProvider.io)
    private var updatesJob: Job? = null
    private var loadingJob: Job? = null

    // Cache of last load request to avoid duplicate work
    @Volatile
    private var lastLoadRequest: Pair<Int, FilterContext>? = null

    @Volatile
    private var walletSetVersion = 0

    private var transactionWallets: List<TransactionWallet> = listOf()
    private var walletsGroupedBySource: List<TransactionWallet> = listOf()

    private val activeAdapters: List<TransactionAdapterWrapper>
        get() {
            val tmpSelectedWallet = selectedWallet
            val tmpSelectedBlockchain = selectedBlockchain
            val activeWallets = when {
                tmpSelectedWallet != null -> listOf(tmpSelectedWallet)
                tmpSelectedBlockchain != null -> walletsGroupedBySource.filter {
                    it.source.blockchain == tmpSelectedBlockchain
                }

                else -> walletsGroupedBySource
            }
            return activeWallets.mapNotNull { adaptersMap[it] }
        }

    private val activeSwapExtraAdapters: List<TransactionAdapterWrapper>
        get() {
            val tmpSelectedWallet = selectedWallet
            val tmpSelectedBlockchain = selectedBlockchain

            val activeWallets = when {
                tmpSelectedWallet != null -> listOf(tmpSelectedWallet)
                tmpSelectedBlockchain != null -> walletsGroupedBySource.filter {
                    it.source.blockchain == tmpSelectedBlockchain
                }

                else -> walletsGroupedBySource
            }
            return activeWallets.mapNotNull { extraSwapAdaptersMap[it] }
        }

    /**
     * Captures current filter context for snapshot comparison.
     * Thread-safe: creates new instance with current values.
     */
    private fun getCurrentFilterContext() = FilterContext(
        transactionType = selectedFilterTransactionType,
        wallet = selectedWallet,
        blockchain = selectedBlockchain,
        contact = contact,
        walletSetVersion = walletSetVersion
    )

    private fun groupWalletsBySource(transactionWallets: List<TransactionWallet>): List<TransactionWallet> {
        val mergedWallets = mutableListOf<TransactionWallet>()

        transactionWallets.forEach { wallet ->
            when (wallet.source.blockchain.type) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dogecoin,
                BlockchainType.PirateCash,
                BlockchainType.Cosanta,
                BlockchainType.Dash,
                BlockchainType.Monero,
                BlockchainType.Zcash -> mergedWallets.add(wallet)

                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.ZkSync,
                BlockchainType.ArbitrumOne,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.Solana,
                BlockchainType.Tron,
                BlockchainType.Stellar,
                BlockchainType.Ton -> {
                    if (mergedWallets.none { it.source == wallet.source }) {
                        mergedWallets.add(TransactionWallet(null, wallet.source, null))
                    }
                }

                is BlockchainType.Unsupported -> Unit
            }
        }
        return mergedWallets

    }

    override fun set(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?,
        contact: Contact?,
    ) {
        val walletsChanged = this.transactionWallets != transactionWallets || adaptersMap.isEmpty()
        if (walletsChanged) {
            this.transactionWallets = transactionWallets
            walletSetVersion++
            _itemsFlow.tryEmit(emptyList())
            walletsGroupedBySource = groupWalletsBySource(transactionWallets)

            // update list of adapters based on wallets
            val currentAdapters = adaptersMap.toMutableMap()
            adaptersMap.clear()
            (transactionWallets + walletsGroupedBySource).distinct().forEach { transactionWallet ->
                var adapter = currentAdapters.remove(transactionWallet)
                if (adapter == null) {
                    adapterManager.getAdapter(transactionWallet.source)?.let {
                        adapter = TransactionAdapterWrapper(
                            transactionsAdapter = it,
                            transactionWallet = transactionWallet,
                            transactionType = selectedFilterTransactionType,
                            contact = contact,
                            pendingRepository = pendingRepository,
                            pendingConverter = pendingConverter,
                            pendingTransactionMatcher = pendingTransactionMatcher,
                        )
                    }
                }

                adapter?.let {
                    adaptersMap[transactionWallet] = it
                }
            }
            currentAdapters.values.forEach(TransactionAdapterWrapper::clear)
            currentAdapters.clear()

            buildExtraSwapAdapters()
        }

        var reload = walletsChanged

        if (selectedWallet != wallet || wallet == null) {
            selectedWallet = wallet
            reload = true
        }

        if (selectedBlockchain != blockchain) {
            selectedBlockchain = blockchain
            reload = true
        }

        if (transactionType != selectedFilterTransactionType) {
            selectedFilterTransactionType = transactionType

            adaptersMap.forEach { (_, transactionAdapterWrapper) ->
                transactionAdapterWrapper.setTransactionType(transactionType)
            }
            reload = true
        }

        if (this.contact != contact) {
            this.contact = contact
            updateContact(contact)
            reload = true
        }

        if (reload) {
            unsubscribeFromUpdates()
            allNormalLoaded.set(false)
            allExtraLoaded.set(false)
            loadedPageNumber = 1
            loadItems(loadedPageNumber)
            subscribeForUpdates()
        }
    }

    /***
     * We need such adapters only fo changenow swaps because they are not typical swaps
     */
    private fun buildExtraSwapAdapters() {
        // update list of adapters based on wallets
        val currentAdapters = extraSwapAdaptersMap.toMutableMap()
        extraSwapAdaptersMap.clear()
        (transactionWallets + walletsGroupedBySource).distinct().forEach { transactionWallet ->
            var adapter = currentAdapters.remove(transactionWallet)
            if (adapter == null) {
                adapterManager.getAdapter(transactionWallet.source)?.let {
                    adapter = TransactionAdapterWrapper(
                        transactionsAdapter = it,
                        transactionWallet = transactionWallet,
                        transactionType = FilterTransactionType.Outgoing,
                        contact = contact,
                        pendingRepository = pendingRepository,
                        pendingConverter = pendingConverter,
                        pendingTransactionMatcher = pendingTransactionMatcher,
                    )
                }
            }

            adapter?.let {
                extraSwapAdaptersMap[transactionWallet] = it
            }
        }
        currentAdapters.values.forEach(TransactionAdapterWrapper::clear)
        currentAdapters.clear()
    }

    private fun updateContact(contact: Contact?) {
        adaptersMap.values.forEach { transactionAdapterWrapper ->
            transactionAdapterWrapper.setContact(contact)
        }
        extraSwapAdaptersMap.values.forEach { transactionAdapterWrapper ->
            transactionAdapterWrapper.setContact(contact)
        }
    }

    override fun loadNext() {
        if (allNormalLoaded.get() &&
            (selectedFilterTransactionType != FilterTransactionType.Swap || allExtraLoaded.get())
        ) return
        loadItems(loadedPageNumber + 1)
    }

    override fun reload() {
        adaptersMap.forEach { (_, transactionAdapterWrapper) ->
            transactionAdapterWrapper.reload()
        }
        unsubscribeFromUpdates()
        allNormalLoaded.set(false)
        allExtraLoaded.set(false)
        loadItems(1)
        subscribeForUpdates()
    }

    override fun cancelPendingLoads() {
        loadingJob?.cancel()
        updatesJob?.cancel()
    }

    private fun unsubscribeFromUpdates() {
        updatesJob?.cancel()
    }

    private fun subscribeForUpdates() {
        updatesJob = coroutineScope.launch {
            activeAdapters
                .map { it.updatedFlow }
                .merge()
                .collect {
                    handleUpdates()
                }
        }
    }

    @Synchronized
    private fun handleUpdates() {
        allNormalLoaded.set(false)
        allExtraLoaded.set(false)
        loadItems(loadedPageNumber)
    }

    private fun loadItems(page: Int) {
        // Capture current filter context for validation
        val requestContext = getCurrentFilterContext()
        val currentRequest = Pair(page, requestContext)

        // Optimization: Skip if exact same request is already in progress
        if (loadingJob?.isActive == true && lastLoadRequest == currentRequest) {
            return
        }

        // Cache this request for future comparison
        lastLoadRequest = currentRequest

        // Cancel previous load if it's a different request
        loadingJob?.cancel()

        val itemsCount = page * itemsPerPage
        val adapters = activeAdapters

        loadingJob = coroutineScope.launch {
            try {
                val records = adapters
                    .map { wrapper -> async {
                        wrapper.get(
                            limit = itemsCount,
                            requestedFilterType = requestContext.transactionType,
                            requestedContact = requestContext.contact
                        )
                    } }
                    .awaitAll()
                    .flatten()

                if (!isActive || requestContext != getCurrentFilterContext()) {
                    return@launch
                }
                var extraRecordsCount = 0
                val extraRecords = if (requestContext.transactionType == FilterTransactionType.Swap) {
                    activeSwapExtraAdapters
                        .map { async {
                            it.get(
                                limit = itemsCount,
                                requestedFilterType = FilterTransactionType.Outgoing,
                                requestedContact = requestContext.contact
                            )
                        } }
                        .awaitAll()
                        .map { transactionRecords ->
                            extraRecordsCount += transactionRecords.size
                            transactionRecords.mapNotNull { record ->
                                if (isMatchingSwapProviderTransaction(record)) {
                                    record
                                } else {
                                    null
                                }
                            }
                        }
                        .flatten()
                } else {
                    emptyList()
                }

                if (!isActive || requestContext != getCurrentFilterContext()) {
                    return@launch
                }

                if (extraRecordsCount < page * itemsPerPage) {
                    allExtraLoaded.set(true)
                }

                handleRecords(records, extraRecords, page)

            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    override fun clear() {
        adaptersMap.values.forEach(TransactionAdapterWrapper::clear)
        adaptersMap.clear()

        extraSwapAdaptersMap.values.forEach(TransactionAdapterWrapper::clear)
        extraSwapAdaptersMap.clear()
        coroutineScope.cancel()
    }

    private fun handleRecords(
        records: List<TransactionRecord>,
        extraRecords: List<TransactionRecord>,
        page: Int
    ) {
        val expectedItemsCount = page * itemsPerPage

        val normalSortedRecords = records
            .sortedDescending()
            .take(expectedItemsCount)

        if (normalSortedRecords.size < expectedItemsCount) {
            allNormalLoaded.set(true)
        }

        val extraSortedRecords = extraRecords
            .sortedDescending()
            .take(expectedItemsCount)

        loadedPageNumber = page
        _itemsFlow.tryEmit((normalSortedRecords + extraSortedRecords).sortedDescending())
    }

    private fun isMatchingSwapProviderTransaction(record: TransactionRecord): Boolean {
        // First check by outgoingRecordUid (fast, already matched)
        swapProviderTransactionsStorage.getByOutgoingRecordUid(record.uid)?.let {
            return true
        }

        // Fall back to matching by token, amount and timestamp
        val shortOutgoingTransactionRecord = record.getShortOutgoingTransactionRecord()
            ?: return false

        val token = shortOutgoingTransactionRecord.token
            ?: return false

        return swapProviderTransactionsStorage.getByCoinUidIn(
            coinUid = token.coin.uid,
            blockchainType = token.blockchainType.uid,
            amountIn = shortOutgoingTransactionRecord.amountOut,
            timestamp = shortOutgoingTransactionRecord.timestamp
        ) != null
    }

    companion object {
        const val itemsPerPage = 20
    }

}

/**
 * Snapshot of filter context at the time of load request.
 * Used to detect filter changes during async loading and discard stale results.
 */
private data class FilterContext(
    val transactionType: FilterTransactionType,
    val wallet: TransactionWallet?,
    val blockchain: Blockchain?,
    val contact: Contact?,
    val walletSetVersion: Int
)

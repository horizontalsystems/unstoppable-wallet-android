package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.managers.CoinManager
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    val transactionWallet: TransactionWallet,
    @Volatile
    private var transactionType: FilterTransactionType,
    @Volatile
    private var contact: Contact?,
    private val pendingRepository: PendingTransactionRepository,
    private val pendingConverter: PendingTransactionConverter,
    private val pendingTransactionMatcher: PendingTransactionMatcher,
) : Clearable {
    // Use MutableSharedFlow for updates
    private val _updatedFlow = MutableSharedFlow<Unit>(replay = 0)
    val updatedFlow: SharedFlow<Unit> get() = _updatedFlow.asSharedFlow()

    // Use StateFlow for transaction records
    private val _transactionRecords = MutableStateFlow<List<TransactionRecord>>(emptyList())
    private val coinManager: CoinManager by inject(CoinManager::class.java)

    // Use StateFlow for allLoaded flag - this is more consistent than MutableSharedFlow
    private val _allLoaded = MutableStateFlow(false)

    // Use SupervisorJob to prevent child failures from cancelling the entire scope
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updatesJob: Job? = null

    val address: String?
        get() = contact
            ?.addresses
            ?.find { it.blockchain == transactionWallet.source.blockchain }
            ?.address

    init {
        subscribeForUpdates()
    }

    fun reload() {
        coroutineScope.launch {
            _transactionRecords.update { emptyList() }
            _allLoaded.value = false
            subscribeForUpdates()
        }
    }

    fun setTransactionType(transactionType: FilterTransactionType) {
        this.transactionType = transactionType
        _transactionRecords.update { emptyList() }
        _allLoaded.value = false
        subscribeForUpdates()
    }

    fun setContact(contact: Contact?) {
        this.contact = contact
        coroutineScope.launch {
            _transactionRecords.update { emptyList() }
            _allLoaded.value = false
            subscribeForUpdates()
        }
    }

    private fun subscribeForUpdates() {
        // Snapshot: capture current filter parameters at subscription time
        val expectedType = transactionType
        val expectedContact = contact
        val expectedAddress = address

        updatesJob?.cancel()

        if (expectedContact != null && expectedAddress == null) return

        updatesJob = coroutineScope.launch {
            val walletId = transactionWallet.source.account.id

            merge(
                transactionsAdapter
                    .getTransactionRecordsFlow(
                        transactionWallet.token,
                        expectedType,     // Use snapshot!
                        expectedAddress   // Use snapshot!
                    ), pendingRepository.getActivePendingFlow(walletId)
            ).collectLatest {
                if (!isActive ||
                    transactionType != expectedType ||
                    contact != expectedContact
                ) {
                    return@collectLatest
                }

                _transactionRecords.update { emptyList() }
                _allLoaded.value = false
                _updatedFlow.emit(Unit)

            }
        }
    }

    suspend fun get(
        limit: Int,
        requestedFilterType: FilterTransactionType,
        requestedContact: Contact?
    ): List<TransactionRecord> {
        // Check if cache is valid for the requested filter
        if (transactionType != requestedFilterType || contact != requestedContact) {
            // Cache is stale for the requested filter - return empty list
            return emptyList()
        }

        val requestedAddress = requestedContact
            ?.addresses
            ?.find { it.blockchain == transactionWallet.source.blockchain }
            ?.address

        return when {
            _transactionRecords.value.size >= limit || _allLoaded.value -> {
                _transactionRecords.value.take(limit)
            }

            requestedContact != null && requestedAddress == null -> {
                emptyList()
            }

            else -> {
                val currentRecords = _transactionRecords.value
                val numberOfRecordsToRequest = limit - currentRecords.size

                // Load data using requested parameters
                val receivedRecords = transactionsAdapter.getTransactions(
                    from = currentRecords.lastOrNull(),
                    token = transactionWallet.token,
                    limit = numberOfRecordsToRequest,
                    transactionType = requestedFilterType,
                    address = requestedAddress
                )

                // Validation: check if parameters haven't changed during the load
                if (transactionType != requestedFilterType || contact != requestedContact) {
                    return emptyList()
                }

                // Parameters still match - safe to save the results
                _allLoaded.value = receivedRecords.size < numberOfRecordsToRequest

                // Merge with pending transactions
                val mergedRecords = mergePendingAndReal(currentRecords + receivedRecords)
                _transactionRecords.value = mergedRecords

                mergedRecords.take(limit)
            }
        }
    }

    private fun getPending(pendingEntities: List<PendingTransactionEntity>): List<TransactionRecord> {
        val blockchainUid = transactionWallet.source.blockchain.type.uid
        val filtered = pendingEntities.filter { it.blockchainTypeUid == blockchainUid }

        return if (transactionWallet.token != null) {
            val token = transactionWallet.token
            filtered
                .filter {
                    it.coinUid == token.coin.uid && it.tokenTypeId == token.type.id
                }
                .map { pendingConverter.convert(it, token) }
        } else {
            filtered
                .mapNotNull {
                    val tokenType = TokenType.fromId(it.tokenTypeId)
                    val token = tryOrNull {
                        coinManager.getToken(TokenQuery(BlockchainType.fromUid(it.blockchainTypeUid), tokenType))
                    } ?: return@mapNotNull null
                    pendingConverter.convert(it, token)
                }
        }
    }

    private suspend fun mergePendingAndReal(realRecords: List<TransactionRecord>): List<TransactionRecord> {
        if (
            transactionType != FilterTransactionType.All &&
            transactionType != FilterTransactionType.Outgoing
        ) {
            // Pending transactions are always for outgoing transactions
            return realRecords
        }

        val walletId = transactionWallet.source.account.id

        return try {
            val pendingEntities = pendingRepository.getPendingForWallet(walletId)
            val pendingRecords = getPending(pendingEntities)
            val filteredPending = filterDuplicatedPending(pendingRecords, realRecords)

            (realRecords + filteredPending).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            // If something fails, return real records only
            realRecords
        }
    }

    private fun filterDuplicatedPending(
        pendingRecords: List<TransactionRecord>,
        realRecords: List<TransactionRecord>,
    ): List<TransactionRecord> {
        val realHashes = realRecords.mapNotNullTo(HashSet()) { record ->
            record.transactionHash.takeIf(String::isNotEmpty)
        }

        return pendingRecords.filter { pending ->
            pending !is PendingTransactionRecord || !hasMatchingRealRecord(pending, realRecords, realHashes)
        }
    }

    private fun hasMatchingRealRecord(
        pending: PendingTransactionRecord,
        realRecords: List<TransactionRecord>,
        realHashes: Set<String>,
    ): Boolean {
        if (pending.transactionHash.isNotEmpty() && pending.transactionHash in realHashes) {
            return true
        }

        return realRecords.any { real ->
            pendingTransactionMatcher.calculateMatchScore(pending, real).isMatch
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}

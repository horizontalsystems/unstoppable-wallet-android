package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.converters.PendingTransactionConverter
import cash.p.terminal.core.managers.CoinManager
import cash.p.terminal.core.managers.LocallyCreatedTransactionRepository
import cash.p.terminal.core.managers.PendingTransactionMatcher
import cash.p.terminal.core.managers.PendingTransactionMatchKind
import cash.p.terminal.core.managers.PendingTransactionRepository
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.abs

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
    private val locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository,
    dispatcherProvider: DispatcherProvider,
) : Clearable {
    private companion object {
        const val CREATED_MARK_MIN_CONFIDENCE = 0.9
    }

    private data class PendingRealMatchCandidate(
        val realIndex: Int,
        val confidence: Double,
        val timestampDifference: Long,
        val kind: PendingTransactionMatchKind,
    )

    private data class PendingRealMatch(
        val pendingIndex: Int,
        val confidence: Double,
        val kind: PendingTransactionMatchKind,
    )

    // Use MutableSharedFlow for updates
    private val _updatedFlow = MutableSharedFlow<Unit>(replay = 0)
    val updatedFlow: SharedFlow<Unit> get() = _updatedFlow.asSharedFlow()

    // Use StateFlow for transaction records
    private val _transactionRecords = MutableStateFlow<List<TransactionRecord>>(emptyList())
    private val coinManager: CoinManager by inject(CoinManager::class.java)

    // Use StateFlow for allLoaded flag - this is more consistent than MutableSharedFlow
    private val _allLoaded = MutableStateFlow(false)

    private val coroutineScope = CoroutineScope(dispatcherProvider.io + SupervisorJob())
    private val getMutex = Mutex()
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
        resetCacheAndResubscribe()
    }

    fun setTransactionType(transactionType: FilterTransactionType) {
        this.transactionType = transactionType
        resetCacheAndResubscribe()
    }

    fun setContact(contact: Contact?) {
        this.contact = contact
        resetCacheAndResubscribe()
    }

    private fun resetCacheAndResubscribe() {
        _transactionRecords.update { emptyList() }
        _allLoaded.value = false
        subscribeForUpdates()
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
    ): List<TransactionRecord> = getMutex.withLock {
        // Check if cache is valid for the requested filter
        if (transactionType != requestedFilterType || contact != requestedContact) {
            // Cache is stale for the requested filter - return empty list
            return@withLock emptyList()
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
                    return@withLock emptyList()
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
                        coinManager.getToken(
                            TokenQuery(
                                BlockchainType.fromUid(it.blockchainTypeUid),
                                tokenType
                            )
                        )
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
            val matchedPendingByReal = matchedPendingByRealIndexes(pendingRecords, realRecords)
            val adjustedRealRecords = adjustMatchedRealRecords(realRecords, pendingRecords, matchedPendingByReal)
            markMatchedRealRecordsCreated(matchedPendingByReal, realRecords, pendingRecords)
            val matchedPendingIndexes = matchedPendingByReal.values.mapTo(HashSet()) { it.pendingIndex }
            val filteredPending = filterDuplicatedPending(
                pendingRecords = pendingRecords,
                realRecords = adjustedRealRecords,
                matchedPendingIndexes = matchedPendingIndexes,
            )

            (adjustedRealRecords + filteredPending).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            // If something fails, return real records only
            realRecords
        }
    }

    private fun adjustMatchedRealRecords(
        realRecords: List<TransactionRecord>,
        pendingRecords: List<TransactionRecord>,
        matchedPendingByReal: Map<Int, PendingRealMatch>,
    ): List<TransactionRecord> {
        return realRecords.mapIndexed { realIndex, real ->
            val match = matchedPendingByReal[realIndex] ?: return@mapIndexed real
            val pending = pendingRecords.getOrNull(match.pendingIndex) as? PendingTransactionRecord
                ?: return@mapIndexed real

            real.withPendingDisplayAmount(pending, match.kind)
        }
    }

    private fun TransactionRecord.withPendingDisplayAmount(
        pending: PendingTransactionRecord,
        matchKind: PendingTransactionMatchKind,
    ): TransactionRecord {
        if (matchKind != PendingTransactionMatchKind.LitecoinMwebPegIn) {
            return this
        }

        val bitcoinRecord = this as? BitcoinTransactionRecord ?: return this
        return bitcoinRecord.withMainAmount(pending.amount.abs().negate())
    }

    private fun filterDuplicatedPending(
        pendingRecords: List<TransactionRecord>,
        realRecords: List<TransactionRecord>,
        matchedPendingIndexes: Set<Int>,
    ): List<TransactionRecord> {
        val duplicatePendingUids = realRecords
            .filterIsInstance<PendingTransactionRecord>()
            .mapTo(HashSet()) { it.uid }

        return pendingRecords.filterIndexed { index, pending ->
            pending !is PendingTransactionRecord ||
                (pending.uid !in duplicatePendingUids && index !in matchedPendingIndexes)
        }
    }

    private suspend fun markMatchedRealRecordsCreated(
        matchedPendingByReal: Map<Int, PendingRealMatch>,
        realRecords: List<TransactionRecord>,
        pendingRecords: List<TransactionRecord>,
    ) {
        val pendingIdsToDelete = mutableListOf<String>()

        matchedPendingByReal.forEach { (realIndex, match) ->
            if (match.confidence < CREATED_MARK_MIN_CONFIDENCE) {
                return@forEach
            }

            val pendingId = pendingRecords.getOrNull(match.pendingIndex)?.uid ?: return@forEach
            val realRecord = realRecords.getOrNull(realIndex) ?: return@forEach

            locallyCreatedTransactionRepository.markCreated(realRecord)
            pendingIdsToDelete.add(pendingId)
        }

        if (pendingIdsToDelete.isNotEmpty()) {
            pendingRepository.deleteByIds(pendingIdsToDelete)
        }
    }

    private fun matchedPendingByRealIndexes(
        pendingRecords: List<TransactionRecord>,
        realRecords: List<TransactionRecord>,
    ): Map<Int, PendingRealMatch> {
        val realCandidates = realRecords.withIndex()
            .filterNot { it.value is PendingTransactionRecord }

        val pendingCandidateMap = pendingRecords.mapIndexedNotNull { pendingIndex, record ->
            val pending = record as? PendingTransactionRecord ?: return@mapIndexedNotNull null
            val candidates = realCandidates.mapNotNull { (realIndex, real) ->
                val matchScore = pendingTransactionMatcher.matchScoreForRealRecord(pending, real)
                if (!matchScore.isMatch) {
                    return@mapNotNull null
                }

                PendingRealMatchCandidate(
                    realIndex = realIndex,
                    confidence = matchScore.confidence,
                    timestampDifference = abs(pending.timestamp - real.timestamp),
                    kind = matchScore.kind,
                )
            }.sortedWith(
                compareByDescending<PendingRealMatchCandidate> { it.confidence }
                    .thenBy { it.timestampDifference }
                    .thenBy { it.realIndex }
            )

            if (candidates.isEmpty()) {
                null
            } else {
                pendingIndex to candidates
            }
        }.toMap()

        val pendingIndexes = pendingCandidateMap.keys.sortedWith(
            compareBy<Int> { pendingCandidateMap.getValue(it).size }
                .thenByDescending { pendingCandidateMap.getValue(it).first().confidence }
                .thenBy { pendingCandidateMap.getValue(it).first().timestampDifference }
                .thenBy { it }
        )

        val matchedPendingByReal = mutableMapOf<Int, PendingRealMatch>()
        pendingIndexes.forEach { pendingIndex ->
            assignRealRecord(
                pendingIndex = pendingIndex,
                pendingCandidateMap = pendingCandidateMap,
                visitedRealIndexes = mutableSetOf(),
                matchedPendingByReal = matchedPendingByReal
            )
        }

        return matchedPendingByReal
    }

    private fun assignRealRecord(
        pendingIndex: Int,
        pendingCandidateMap: Map<Int, List<PendingRealMatchCandidate>>,
        visitedRealIndexes: MutableSet<Int>,
        matchedPendingByReal: MutableMap<Int, PendingRealMatch>,
    ): Boolean {
        val candidates = pendingCandidateMap[pendingIndex] ?: return false

        for (candidate in candidates) {
            if (!visitedRealIndexes.add(candidate.realIndex)) {
                continue
            }

            val assignedMatch = matchedPendingByReal[candidate.realIndex]
            if (assignedMatch == null || assignRealRecord(
                    pendingIndex = assignedMatch.pendingIndex,
                    pendingCandidateMap = pendingCandidateMap,
                    visitedRealIndexes = visitedRealIndexes,
                    matchedPendingByReal = matchedPendingByReal
                )
            ) {
                matchedPendingByReal[candidate.realIndex] = PendingRealMatch(
                    pendingIndex = pendingIndex,
                    confidence = candidate.confidence,
                    kind = candidate.kind,
                )
                return true
            }
        }

        return false
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}

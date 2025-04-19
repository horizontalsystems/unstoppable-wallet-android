package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.wallet.Clearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    val transactionWallet: TransactionWallet,
    private var transactionType: FilterTransactionType,
    private var contact: Contact?
) : Clearable {
    // Use MutableSharedFlow for updates
    private val _updatedFlow = MutableSharedFlow<Unit>(replay = 0)
    val updatedFlow: SharedFlow<Unit> get() = _updatedFlow.asSharedFlow()

    // Use StateFlow for transaction records
    private val _transactionRecords = MutableStateFlow<List<TransactionRecord>>(emptyList())

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
        coroutineScope.launch {
            _transactionRecords.update { emptyList() }
            _allLoaded.value = false
            subscribeForUpdates()
        }
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
        updatesJob?.cancel()

        if (contact != null && address == null) return

        updatesJob = coroutineScope.launch {
            transactionsAdapter
                .getTransactionRecordsFlow(transactionWallet.token, transactionType, address)
                .collect {
                    _transactionRecords.update { emptyList() }
                    _allLoaded.value = false
                    _updatedFlow.emit(Unit)
                }
        }
    }

    suspend fun get(limit: Int): List<TransactionRecord> = when {
        _transactionRecords.value.size >= limit || _allLoaded.value -> _transactionRecords.value.take(
            limit
        )

        contact != null && address == null -> emptyList()
        else -> {
            val currentRecords = _transactionRecords.value
            val numberOfRecordsToRequest = limit - currentRecords.size
            val receivedRecords = transactionsAdapter.getTransactions(
                from = currentRecords.lastOrNull(),
                token = transactionWallet.token,
                limit = numberOfRecordsToRequest,
                transactionType = transactionType,
                address = address
            )

            // Use StateFlow's value setter for atomic update
            _allLoaded.value = receivedRecords.size < numberOfRecordsToRequest

            // Update the StateFlow with new records
            val updatedRecords = currentRecords + receivedRecords
            _transactionRecords.value = updatedRecords // More efficient for single atomic updates

            updatedRecords
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
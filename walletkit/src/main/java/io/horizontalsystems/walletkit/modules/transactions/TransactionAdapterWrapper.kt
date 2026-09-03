package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet,
    private var transactionType: FilterTransactionType,
    private var contact: Contact?
) : Clearable {
    private val _updatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val updatedFlow: Flow<Unit> get() = _updatedFlow

    private val transactionRecords = CopyOnWriteArrayList<TransactionRecord>()
    private var allLoaded = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
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
        transactionRecords.clear()
        allLoaded = false
        subscribeForUpdates()
    }

    fun setTransactionType(transactionType: FilterTransactionType) {
        this.transactionType = transactionType
        transactionRecords.clear()
        allLoaded = false
        subscribeForUpdates()
    }

    fun setContact(contact: Contact?) {
        this.contact = contact
        transactionRecords.clear()
        allLoaded = false
        subscribeForUpdates()
    }

    private fun subscribeForUpdates() {
        updatesJob?.cancel()

        if (contact != null && address == null) return

        updatesJob = coroutineScope.launch {
            transactionsAdapter
                .getTransactionRecordsFlow(transactionWallet.token, transactionType, address)
                .collect {
                    transactionRecords.clear()
                    allLoaded = false
                    _updatedFlow.tryEmit(Unit)
                }
        }
    }

    suspend fun get(limit: Int): List<TransactionRecord> = when {
        transactionRecords.size >= limit || allLoaded -> transactionRecords.take(limit)
        contact != null && address == null -> listOf()
        else -> {
            val numberOfRecordsToRequest = limit - transactionRecords.size
            val result = transactionsAdapter.getTransactions(
                transactionRecords.lastOrNull(),
                transactionWallet.token,
                numberOfRecordsToRequest,
                transactionType,
                address
            )
            allLoaded = result.size < numberOfRecordsToRequest
            transactionRecords.addAll(result)
            transactionRecords.toList()
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}

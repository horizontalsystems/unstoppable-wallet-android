package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet,
    private var transactionType: FilterTransactionType,
    private var contact: Contact?
) : Clearable {
    private val updatedSubject = PublishSubject.create<Unit>()
    val updatedObservable: Observable<Unit> get() = updatedSubject

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
                    updatedSubject.onNext(Unit)
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

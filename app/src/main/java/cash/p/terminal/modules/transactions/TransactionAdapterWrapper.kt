package cash.p.terminal.modules.transactions

import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
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
                .getTransactionRecordsFlowable(transactionWallet.token, transactionType, address)
                .asFlow()
                .collect {
                    transactionRecords.clear()
                    allLoaded = false
                    updatedSubject.onNext(Unit)
                }
        }
    }

    fun get(limit: Int): Single<List<TransactionRecord>> = when {
        transactionRecords.size >= limit || allLoaded -> Single.just(transactionRecords.take(limit))
        contact != null && address == null -> Single.just(listOf())
        else -> {
            val numberOfRecordsToRequest = limit - transactionRecords.size
            transactionsAdapter
                .getTransactionsAsync(
                    transactionRecords.lastOrNull(),
                    transactionWallet.token,
                    numberOfRecordsToRequest,
                    transactionType,
                    address
                )
                .map {
                    allLoaded = it.size < numberOfRecordsToRequest
                    transactionRecords.addAll(it)

                    transactionRecords
                }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}

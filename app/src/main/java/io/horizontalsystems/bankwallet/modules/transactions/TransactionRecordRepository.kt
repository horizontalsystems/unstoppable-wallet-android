package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: TransactionAdapterManager,
) : ITransactionRecordRepository {

    private var selectedFilterTransactionType: FilterTransactionType = FilterTransactionType.All

    private var selectedWallet: TransactionWallet? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    override val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    private var loadedPageNumber = 0
    private val items = CopyOnWriteArrayList<TransactionRecord>()
    private val loading = AtomicBoolean(false)
    private var allLoaded = AtomicBoolean(false)
    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val disposables = CompositeDisposable()
    private var disposableUpdates: Disposable? = null

    private var walletsGroupedBySource: List<TransactionWallet> = listOf()

    private val activeAdapters: List<TransactionAdapterWrapper>
        get() {
            val activeWallets = selectedWallet?.let { listOf(it) } ?: walletsGroupedBySource
            return activeWallets.mapNotNull { adaptersMap[it] }
        }

    private fun groupWalletsBySource(transactionWallets: List<TransactionWallet>): List<TransactionWallet> {
        val mergedWallets = mutableListOf<TransactionWallet>()

        transactionWallets.forEach { wallet ->
            when (wallet.source.blockchain) {
                TransactionSource.Blockchain.Bitcoin,
                TransactionSource.Blockchain.BitcoinCash,
                TransactionSource.Blockchain.Litecoin,
                TransactionSource.Blockchain.Dash,
                TransactionSource.Blockchain.Zcash,
                is TransactionSource.Blockchain.Bep2 -> mergedWallets.add(wallet)
                TransactionSource.Blockchain.Ethereum,
                TransactionSource.Blockchain.BinanceSmartChain -> {
                    if (mergedWallets.none { it.source == wallet.source }) {
                        mergedWallets.add(TransactionWallet(null, wallet.source, null))
                    }
                }
            }
        }
        return mergedWallets

    }

    override fun setWallets(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
    ) {
        this.walletsGroupedBySource = groupWalletsBySource(transactionWallets)

        // update list of adapters based on wallets
        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()
        (transactionWallets + this.walletsGroupedBySource).distinct().forEach { transactionWallet ->
            var adapter = currentAdapters.remove(transactionWallet)
            if (adapter == null) {
                adapterManager.getAdapter(transactionWallet.source)?.let {
                    adapter = TransactionAdapterWrapper(it, transactionWallet, selectedFilterTransactionType)
                }
            }

            adapter?.let {
                adaptersMap[transactionWallet] = it
            }
        }
        currentAdapters.values.forEach(TransactionAdapterWrapper::clear)
        currentAdapters.clear()

        var reload = false

        if (selectedWallet != wallet || wallet == null) {
            selectedWallet = wallet
            reload = true
        }

        if (transactionType != selectedFilterTransactionType) {
            selectedFilterTransactionType = transactionType

            adaptersMap.forEach { (_, transactionAdapterWrapper) ->
                transactionAdapterWrapper.setTransactionType(transactionType)
            }
            reload = true
        }

        if (reload) {
            unsubscribeFromUpdates()
            allLoaded.set(false)
            loadItems(1)
            subscribeForUpdates()
        }
    }

    override fun setSelectedWallet(transactionWallet: TransactionWallet?) {
        selectedWallet = transactionWallet

        unsubscribeFromUpdates()
        allLoaded.set(false)
        loadItems(1)
        subscribeForUpdates()
    }

    override fun setTransactionType(transactionType: FilterTransactionType) {
        selectedFilterTransactionType = transactionType

        adaptersMap.forEach { (_, transactionAdapterWrapper) ->
            transactionAdapterWrapper.setTransactionType(transactionType)
        }

        unsubscribeFromUpdates()
        allLoaded.set(false)
        loadItems(1)
        subscribeForUpdates()
    }

    override fun loadNext() {
        if (!allLoaded.get()) {
            loadItems(loadedPageNumber + 1)
        }
    }

    private fun unsubscribeFromUpdates() {
        disposableUpdates?.dispose()
    }

    private fun subscribeForUpdates() {
        disposableUpdates = Observable
            .merge(activeAdapters.map { it.updatedObservable })
            .subscribeIO {
                handleUpdates()
            }
    }

    @Synchronized
    private fun handleUpdates() {
        allLoaded.set(false)
        loadItems(loadedPageNumber)
    }

    private fun loadItems(page: Int) {
        if (loading.get()) return
        loading.set(true)

        val itemsCount = page * itemsPerPage

        val sources = activeAdapters.map { it.get(itemsCount) }
        val recordsObservable = when {
            sources.isEmpty() -> Single.just(listOf())
            else -> Single.zip(sources) {
                it as Array<List<TransactionRecord>>
                it.toList().flatten()
            }
        }

        recordsObservable
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .doFinally {
                loading.set(false)
            }
            .subscribe { records ->
                handleRecords(records, page)
            }
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        adaptersMap.values.forEach(TransactionAdapterWrapper::clear)
        adaptersMap.clear()
        disposables.clear()
        disposableUpdates?.dispose()
    }

    @Synchronized
    private fun handleRecords(records: List<TransactionRecord>, page: Int) {
        val expectedItemsCount = page * itemsPerPage

        records
            .sortedDescending()
            .take(expectedItemsCount)
            .let {
                if (it.size < expectedItemsCount) {
                    allLoaded.set(true)
                }

                items.clear()
                items.addAll(it)
                itemsSubject.onNext(items)

                loadedPageNumber = page
            }
    }

    companion object {
        const val itemsPerPage = 10
    }

}
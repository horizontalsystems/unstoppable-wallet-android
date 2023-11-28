package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
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
    private var selectedBlockchain: Blockchain? = null

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

    private fun groupWalletsBySource(transactionWallets: List<TransactionWallet>): List<TransactionWallet> {
        val mergedWallets = mutableListOf<TransactionWallet>()

        transactionWallets.forEach { wallet ->
            when (wallet.source.blockchain.type) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> mergedWallets.add(wallet)
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.ArbitrumOne,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.Solana,
                BlockchainType.Tron,
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

    override fun setWallets(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?,
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

        if (reload) {
            unsubscribeFromUpdates()
            allLoaded.set(false)
            loadItems(1)
            subscribeForUpdates()
        }
    }

    override fun setWalletAndBlockchain(transactionWallet: TransactionWallet?, blockchain: Blockchain?) {
        selectedWallet = transactionWallet
        this.selectedBlockchain = blockchain

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

    override fun reload() {
        adaptersMap.forEach { (_, transactionAdapterWrapper) ->
            transactionAdapterWrapper.reload()
        }
        unsubscribeFromUpdates()
        allLoaded.set(false)
        loadItems(1)
        subscribeForUpdates()
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
                it.filterIsInstance<List<TransactionRecord>>().toList().flatten()
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
        const val itemsPerPage = 20
    }

}
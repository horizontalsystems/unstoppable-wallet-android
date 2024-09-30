package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: TransactionAdapterManager,
) : ITransactionRecordRepository {

    private var selectedFilterTransactionType: FilterTransactionType = FilterTransactionType.All

    private var selectedWallet: TransactionWallet? = null
    private var selectedBlockchain: Blockchain? = null
    private var contact: Contact? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    override val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    private var loadedPageNumber = 0
    private val items = CopyOnWriteArrayList<TransactionRecord>()
    private val loading = AtomicBoolean(false)
    private var allLoaded = AtomicBoolean(false)
    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var updatesJob: Job? = null

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
                BlockchainType.Base,
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

    override fun set(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?,
        contact: Contact?,
    ) {
        if (this.transactionWallets != transactionWallets || adaptersMap.isEmpty()) {
            this.transactionWallets = transactionWallets
            walletsGroupedBySource = groupWalletsBySource(transactionWallets)

            // update list of adapters based on wallets
            val currentAdapters = adaptersMap.toMutableMap()
            adaptersMap.clear()
            (transactionWallets + walletsGroupedBySource).distinct().forEach { transactionWallet ->
                var adapter = currentAdapters.remove(transactionWallet)
                if (adapter == null) {
                    adapterManager.getAdapter(transactionWallet.source)?.let {
                        adapter = TransactionAdapterWrapper(
                            it,
                            transactionWallet,
                            selectedFilterTransactionType,
                            contact
                        )
                    }
                }

                adapter?.let {
                    adaptersMap[transactionWallet] = it
                }
            }
            currentAdapters.values.forEach(TransactionAdapterWrapper::clear)
            currentAdapters.clear()
        }

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

        if (this.contact != contact) {
            this.contact = contact
            adaptersMap.forEach { (_, transactionAdapterWrapper) ->
                transactionAdapterWrapper.setContact(contact)
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
        updatesJob?.cancel()
    }

    private fun subscribeForUpdates() {
        updatesJob = coroutineScope.launch {
            activeAdapters
                .map { it.updatedObservable.asFlow() }
                .merge()
                .collect {
                    handleUpdates()
                }
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

        coroutineScope.launch {
            try {
                val records = activeAdapters
                    .map { async { it.get(itemsCount).await() } }
                    .awaitAll()
                    .flatten()

                handleRecords(records, page)
            } catch (e: Throwable) {

            } finally {
                loading.set(false)
            }
        }
    }

    override fun clear() {
        adaptersMap.values.forEach(TransactionAdapterWrapper::clear)
        adaptersMap.clear()
        coroutineScope.cancel()
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
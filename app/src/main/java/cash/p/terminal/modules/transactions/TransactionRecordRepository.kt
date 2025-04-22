package cash.p.terminal.modules.transactions

import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.storage.ChangeNowTransactionsStorage
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.getShortOutgoingTransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
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
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: TransactionAdapterManager,
    private val changeNowTransactionsStorage: ChangeNowTransactionsStorage,
) : ITransactionRecordRepository {

    private var selectedFilterTransactionType: FilterTransactionType = FilterTransactionType.All

    private var selectedWallet: TransactionWallet? = null
    private var selectedBlockchain: Blockchain? = null
    private var contact: Contact? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    override val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    @Volatile
    private var loadedPageNumber = 0
    private val loading = AtomicBoolean(false)

    private var allNormalLoaded = AtomicBoolean(false)
    private var allExtraLoaded = AtomicBoolean(false)

    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()
    private val extraSwapAdaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
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

    private fun groupWalletsBySource(transactionWallets: List<TransactionWallet>): List<TransactionWallet> {
        val mergedWallets = mutableListOf<TransactionWallet>()

        transactionWallets.forEach { wallet ->
            when (wallet.source.blockchain.type) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dogecoin,
                BlockchainType.Cosanta,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> mergedWallets.add(wallet)

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
                            transactionsAdapter = it,
                            transactionWallet = transactionWallet,
                            transactionType = selectedFilterTransactionType,
                            contact = contact
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
            allNormalLoaded.set(false)
            allExtraLoaded.set(false)
            loadItems(1)
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
                        contact = contact
                    )
                }
            }

            adapter?.let {
                extraSwapAdaptersMap[transactionWallet] = it
            }
        }
        currentAdapters.values.forEach(TransactionAdapterWrapper::clear)
        currentAdapters.clear()


        if (this.contact != contact) {
            extraSwapAdaptersMap.forEach { (_, transactionAdapterWrapper) ->
                transactionAdapterWrapper.setContact(contact)
            }
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
        if (loading.get()) return
        loading.set(true)

        val itemsCount = page * itemsPerPage

        coroutineScope.launch {
            try {
                val records = activeAdapters
                    .map { async { it.get(itemsCount) } }
                    .awaitAll()
                    .flatten()
                var extraRecordsCount = 0
                val extraRecords =
                    if (selectedFilterTransactionType == FilterTransactionType.Swap) {
                        activeSwapExtraAdapters
                            .map { async { it.get(itemsCount) } }
                            .awaitAll()
                            .map { transactionRecords ->
                                extraRecordsCount += transactionRecords.size
                                transactionRecords.map { record ->
                                    val shortOutgoingTransactionRecord =
                                        record.getShortOutgoingTransactionRecord()
                                    if (shortOutgoingTransactionRecord?.token != null &&
                                        changeNowTransactionsStorage.getByTokenIn(
                                            token = shortOutgoingTransactionRecord.token,
                                            amountIn = shortOutgoingTransactionRecord.amountOut,
                                            timestamp = shortOutgoingTransactionRecord.timestamp
                                        ) != null
                                    ) {
                                        record
                                    } else {
                                        null
                                    }
                                }
                            }
                            .flatten().filterNotNull()
                    } else {
                        emptyList()
                    }
                if (extraRecordsCount < page * itemsPerPage) {
                    allExtraLoaded.set(true)
                }

                handleRecords(records, extraRecords, page)
            } catch (e: Throwable) {

            } finally {
                loading.set(false)
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
        itemsSubject.onNext((normalSortedRecords + extraSortedRecords).sortedDescending())
    }

    companion object {
        const val itemsPerPage = 20
    }

}
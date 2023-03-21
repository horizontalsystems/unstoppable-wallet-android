package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.nftUids
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class TransactionsService(
    private val transactionRecordRepository: ITransactionRecordRepository,
    private val rateRepository: TransactionsRateRepository,
    private val transactionSyncStateRepository: TransactionSyncStateRepository,
    private val contactsRepository: ContactsRepository,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: IWalletManager,
    private val transactionFilterService: TransactionFilterService,
    private val nftMetadataService: NftMetadataService
) : Clearable {
    val filterResetEnabled by transactionFilterService::resetEnabled

    private val itemsSubject = BehaviorSubject.create<List<TransactionItem>>()
    val itemsObservable: Observable<List<TransactionItem>> get() = itemsSubject

    val syncingObservable get() = transactionSyncStateRepository.syncingObservable

    private val blockchainSubject = BehaviorSubject.create<Pair<List<Blockchain?>, Blockchain?>>()
    val blockchainObservable get() = blockchainSubject

    private val typesSubject = BehaviorSubject.create<Pair<List<FilterTransactionType>, FilterTransactionType>>()
    val typesObservable get() = typesSubject

    private val walletsSubject = BehaviorSubject.create<Pair<List<TransactionWallet?>, TransactionWallet?>>()
    val walletsObservable get() = walletsSubject

    private val disposables = CompositeDisposable()
    private val transactionItems = CopyOnWriteArrayList<TransactionItem>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        handleUpdatedWallets(walletManager.activeWallets)

        transactionAdapterManager.adaptersReadyObservable
            .subscribeIO {
                handleUpdatedWallets(walletManager.activeWallets)
            }
            .let {
                disposables.add(it)
            }

        transactionRecordRepository.itemsObservable
            .subscribeIO {
                handleUpdatedRecords(it)
            }
            .let {
                disposables.add(it)
            }

        rateRepository.dataExpiredObservable
            .subscribeIO {
                handleUpdatedHistoricalRates()
            }
            .let {
                disposables.add(it)
            }

        rateRepository.historicalRateObservable
            .subscribeIO {
                handleUpdatedHistoricalRate(it.first, it.second)
            }
            .let {
                disposables.add(it)
            }

        transactionSyncStateRepository.lastBlockInfoObservable
            .subscribeIO { (source, lastBlockInfo) ->
                handleLastBlockInfo(source, lastBlockInfo)
            }
            .let {
                disposables.add(it)
            }

        coroutineScope.launch {
            nftMetadataService.assetsBriefMetadataFlow.collect {
                handle(it)
            }
        }

        coroutineScope.launch {
            contactsRepository.contactsFlow.collect {
                handleContactsUpdate()
            }
        }
    }

    @Synchronized
    private fun handleContactsUpdate() {
        val tmpList = mutableListOf<TransactionItem>()
        transactionItems.forEach {
            tmpList.add(it.copy())
        }

        transactionItems.clear()
        transactionItems.addAll(tmpList)

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handle(assetBriefMetadataMap: Map<NftUid, NftAssetBriefMetadata>) {
        var updated = false
        transactionItems.forEachIndexed { index, item ->
            val tmpMetadata = item.nftMetadata.toMutableMap()
            item.record.nftUids.forEach { nftUid ->
                assetBriefMetadataMap[nftUid]?.let {
                    tmpMetadata[nftUid] = it
                }
            }
            transactionItems[index] = item.copy(nftMetadata = tmpMetadata)
            updated = true
        }

        if (updated) {
            itemsSubject.onNext(transactionItems)
        }
    }

    @Synchronized
    private fun handleLastBlockInfo(source: TransactionSource, lastBlockInfo: LastBlockInfo) {
        var updated = false
        transactionItems.forEachIndexed { index, item ->
            if (item.record.source == source && item.record.changedBy(item.lastBlockInfo, lastBlockInfo)) {
                transactionItems[index] = item.copy(lastBlockInfo = lastBlockInfo)
                updated = true
            }
        }

        if (updated) {
            itemsSubject.onNext(transactionItems)
        }
    }

    @Synchronized
    private fun handleUpdatedHistoricalRate(key: HistoricalRateKey, rate: CurrencyValue) {
        var updated = false
        for (i in 0 until transactionItems.size) {
            val item = transactionItems[i]

            item.record.mainValue?.let { mainValue ->
                mainValue.decimalValue?.let { decimalValue ->
                    if (mainValue.coin?.uid == key.coinUid && item.record.timestamp == key.timestamp) {
                        val currencyValue = CurrencyValue(rate.currency, decimalValue * rate.value)

                        transactionItems[i] = item.copy(currencyValue = currencyValue)
                        updated = true
                    }
                }
            }
        }

        if (updated) {
            itemsSubject.onNext(transactionItems)
        }
    }

    @Synchronized
    private fun handleUpdatedHistoricalRates() {
        for (i in 0 until transactionItems.size) {
            val item = transactionItems[i]
            val currencyValue = getCurrencyValue(item.record)

            transactionItems[i] = item.copy(currencyValue = currencyValue)
        }

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedRecords(transactionRecords: List<TransactionRecord>) {
        val tmpList = mutableListOf<TransactionItem>()

        val nftUids = transactionRecords.nftUids
        val nftMetadata = nftMetadataService.assetsBriefMetadata(nftUids)

        val missingNftUids = nftUids.subtract(nftMetadata.keys)
        if (missingNftUids.isNotEmpty()) {
            coroutineScope.launch {
                nftMetadataService.fetch(missingNftUids)
            }
        }

        val newRecords = mutableListOf<TransactionRecord>()
        transactionRecords.forEach { record ->
            var transactionItem = transactionItems.find { it.record == record }
            if (transactionItem == null) {
                newRecords.add(record)
            }

            if (record.spam) return@forEach

            transactionItem = if (transactionItem == null) {
                val lastBlockInfo = transactionSyncStateRepository.getLastBlockInfo(record.source)
                val currencyValue = getCurrencyValue(record)

                TransactionItem(record, currencyValue, lastBlockInfo, nftMetadata)
            } else {
                transactionItem.copy(record = record)
            }

            tmpList.add(transactionItem)
        }

        if (newRecords.isNotEmpty() && newRecords.all { it.spam }) {
            loadNext()
        } else {
            transactionItems.clear()
            transactionItems.addAll(tmpList)
            itemsSubject.onNext(transactionItems)
        }
    }

    private fun getCurrencyValue(record: TransactionRecord): CurrencyValue? {
        val decimalValue = record.mainValue?.decimalValue ?: return null
        val coinUid = record.mainValue?.coin?.uid ?: return null

        return rateRepository.getHistoricalRate(HistoricalRateKey(coinUid, record.timestamp))
            ?.let { rate -> CurrencyValue(rate.currency, decimalValue * rate.value) }
    }

    @Synchronized
    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        transactionFilterService.setWallets(wallets)

        val transactionWallets = transactionFilterService.getTransactionWallets()

        transactionSyncStateRepository.setTransactionWallets(transactionWallets.filterNotNull())
        transactionRecordRepository.setWallets(
            transactionWallets.filterNotNull(),
            transactionFilterService.selectedWallet,
            transactionFilterService.selectedTransactionType,
            transactionFilterService.selectedBlockchain,
        )

        walletsSubject.onNext(Pair(transactionWallets, transactionFilterService.selectedWallet))
        typesSubject.onNext(Pair(transactionFilterService.getFilterTypes(), transactionFilterService.selectedTransactionType))
        blockchainSubject.onNext(Pair(transactionFilterService.getBlockchains(), transactionFilterService.selectedBlockchain))
    }

    override fun clear() {
        disposables.clear()

        transactionRecordRepository.clear()
        rateRepository.clear()
        transactionSyncStateRepository.clear()
        coroutineScope.cancel()
    }

    private val executorService = Executors.newCachedThreadPool()

    fun setFilterType(f: FilterTransactionType) {
        executorService.submit {
            typesSubject.onNext(Pair(transactionFilterService.getFilterTypes(), f))
            transactionFilterService.setSelectedTransactionType(f)
            transactionRecordRepository.setTransactionType(f)
        }
    }

    fun setFilterBlockchain(blockchain: Blockchain?) {
        executorService.submit {
            blockchainSubject.onNext(Pair(transactionFilterService.getBlockchains(), blockchain))
            transactionFilterService.setSelectedBlockchain(blockchain)

            val selectedWallet = transactionFilterService.selectedWallet
            walletsSubject.onNext(Pair(transactionFilterService.getTransactionWallets(), selectedWallet))

            transactionRecordRepository.setWalletAndBlockchain(selectedWallet, blockchain)
        }
    }

    fun setFilterCoin(w: TransactionWallet?) {
        executorService.submit {
            walletsSubject.onNext(Pair(transactionFilterService.getTransactionWallets(), w))
            transactionFilterService.setSelectedWallet(w)

            val selectedBlockchain = transactionFilterService.selectedBlockchain
            blockchainSubject.onNext(Pair(transactionFilterService.getBlockchains(), selectedBlockchain))

            transactionRecordRepository.setWalletAndBlockchain(w, selectedBlockchain)
        }
    }

    fun resetFilters() {
        executorService.submit {
            transactionFilterService.reset()

            val transactionType = transactionFilterService.selectedTransactionType
            val blockchain = transactionFilterService.selectedBlockchain
            val wallet = transactionFilterService.selectedWallet
            val transactionWallets = transactionFilterService.getTransactionWallets()

            typesSubject.onNext(Pair(transactionFilterService.getFilterTypes(), transactionType))
            blockchainSubject.onNext(Pair(transactionFilterService.getBlockchains(), blockchain))
            walletsSubject.onNext(Pair(transactionWallets, wallet))

            transactionRecordRepository.setWallets(
                transactionWallets.filterNotNull(),
                wallet,
                transactionType,
                blockchain,
            )
        }
    }

    fun loadNext() {
        executorService.submit {
            transactionRecordRepository.loadNext()
        }
    }

    fun fetchRateIfNeeded(recordUid: String) {
        executorService.submit {
            transactionItems.find { it.record.uid == recordUid }?.let { transactionItem ->
                if (transactionItem.currencyValue == null) {
                    transactionItem.record.mainValue?.coin?.uid?.let { coinUid ->
                        rateRepository.fetchHistoricalRate(HistoricalRateKey(coinUid, transactionItem.record.timestamp))
                    }
                }
            }
        }
    }

    fun getTransactionItem(recordUid: String): TransactionItem? {
        return transactionItems.find { it.record.uid == recordUid }
    }
}

package cash.p.terminal.modules.balance.token

import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.nftUids
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.HistoricalRateKey
import cash.p.terminal.modules.transactions.ITransactionRecordRepository
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionItem
import cash.p.terminal.modules.transactions.TransactionSyncStateRepository
import cash.p.terminal.modules.transactions.TransactionWallet
import cash.p.terminal.modules.transactions.TransactionsRateRepository
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.Executors

class TokenTransactionsService(
    private val wallet: Wallet,
    private val rateRepository: TransactionsRateRepository,
    private val transactionSyncStateRepository: TransactionSyncStateRepository,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val nftMetadataService: NftMetadataService,
    private val spamManager: SpamManager,
) : Clearable {
    private val transactionRecordRepository: ITransactionRecordRepository by inject(
        ITransactionRecordRepository::class.java
    )
    private val _transactionItems = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactionItemsFlow: StateFlow<List<TransactionItem>> = _transactionItems.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        coroutineScope.launch {
            transactionRecordRepository.itemsObservable.asFlow().collect {
                handleUpdatedRecords(it)
            }
        }
        coroutineScope.launch {
            rateRepository.dataExpiredFlow.collect {
                handleUpdatedHistoricalRates()
            }
        }
        coroutineScope.launch {
            rateRepository.historicalRateFlow.collect {
                handleUpdatedHistoricalRate(it.first, it.second)
            }
        }
        coroutineScope.launch {
            transactionSyncStateRepository.lastBlockInfoFlow
                .collect { (source, lastBlockInfo) ->
                    handleLastBlockInfo(source, lastBlockInfo)
                }
        }
        coroutineScope.launch {
            nftMetadataService.assetsBriefMetadataFlow.collect {
                handle(it)
            }
        }
        coroutineScope.launch {
            transactionAdapterManager.initializationFlow
                .filter { it }
                .onEach {
                    handleInitialization()
                }
                .first()
        }
    }

    private fun handleInitialization() {
        val transactionWallet =
            TransactionWallet(wallet.token, wallet.transactionSource, wallet.badge)

        transactionSyncStateRepository.setTransactionWallets(listOf(transactionWallet))
        transactionRecordRepository.set(
            transactionWallets = listOf(transactionWallet),
            wallet = transactionWallet,
            transactionType = FilterTransactionType.All,
            blockchain = null,
            contact = null
        )
    }

    private fun handle(assetBriefMetadataMap: Map<NftUid, NftAssetBriefMetadata>) {
        // Original behavior: always rebuild items and emit if list not empty
        // (updated was set to true unconditionally in the loop)
        _transactionItems.update { currentList ->
            if (currentList.isEmpty()) return@update currentList
            currentList.map { item ->
                val updatedMetadata = item.nftMetadata.toMutableMap()
                item.record.nftUids.forEach { nftUid ->
                    assetBriefMetadataMap[nftUid]?.let { updatedMetadata[nftUid] = it }
                }
                item.copy(nftMetadata = updatedMetadata)
            }
        }
    }

    private fun handleLastBlockInfo(source: TransactionSource, lastBlockInfo: LastBlockInfo) {
        _transactionItems.update { currentList ->
            var updated = false
            val newList = currentList.map { item ->
                if (item.record.source == source && item.record.changedBy(item.lastBlockInfo, lastBlockInfo)) {
                    updated = true
                    item.copy(lastBlockInfo = lastBlockInfo)
                } else {
                    item
                }
            }
            if (updated) newList else currentList
        }
    }

    private fun handleUpdatedHistoricalRate(key: HistoricalRateKey, rate: CurrencyValue) {
        _transactionItems.update { currentList ->
            var updated = false
            val newList = currentList.map { item ->
                val mainValue = item.record.mainValue
                val decimalValue = mainValue?.decimalValue
                if (decimalValue != null && mainValue.coin?.uid == key.coinUid && item.record.timestamp == key.timestamp) {
                    updated = true
                    val currencyValue = CurrencyValue(rate.currency, decimalValue * rate.value)
                    item.copy(currencyValue = currencyValue)
                } else {
                    item
                }
            }
            if (updated) newList else currentList
        }
    }

    private fun handleUpdatedHistoricalRates() {
        _transactionItems.update { currentList ->
            currentList.map { item ->
                item.copy(currencyValue = getCurrencyValue(item.record))
            }
        }
    }

    private suspend fun handleUpdatedRecords(transactionRecords: List<TransactionRecord>) {
        val nftUids = transactionRecords.nftUids
        val nftMetadata = nftMetadataService.assetsBriefMetadata(nftUids)

        val missingNftUids = nftUids.subtract(nftMetadata.keys)
        if (missingNftUids.isNotEmpty()) {
            coroutineScope.launch {
                nftMetadataService.fetch(missingNftUids)
            }
        }

        val currentItems = _transactionItems.value
        val newRecords = transactionRecords.filter { record ->
            currentItems.none { it.record == record }
        }

        if (newRecords.isNotEmpty() && newRecords.all { it.spam }) {
            loadNext()
            return
        }

        _transactionItems.update { latestItems ->
            transactionRecords.mapNotNull { record ->
                val existingItem = latestItems.find { it.record == record }

                if (record.spam && spamManager.hideSuspiciousTx) return@mapNotNull null

                if (existingItem == null) {
                    val lastBlockInfo = transactionSyncStateRepository.getLastBlockInfo(record.source)
                    val currencyValue = getCurrencyValue(record)
                    TransactionItem(record, currencyValue, lastBlockInfo, nftMetadata)
                } else {
                    existingItem.copy(record = record)
                }
            }
        }
    }

    private fun getCurrencyValue(record: TransactionRecord): CurrencyValue? {
        val decimalValue = record.mainValue?.decimalValue ?: return null
        val coinUid = record.mainValue?.coin?.uid ?: return null

        return rateRepository.getHistoricalRate(HistoricalRateKey(coinUid, record.timestamp))
            ?.let { rate -> CurrencyValue(rate.currency, decimalValue * rate.value) }
    }

    private val executorService = Executors.newCachedThreadPool()

    fun refreshList() {
        // Original behavior:
        // - forceLoadData=true: copy items (new references), then emit
        // - forceLoadData=false: emit same items (same references)
        //
        // With StateFlow, emission only happens if value changes (structural equality).
        // To preserve the "always emit" behavior for callers that depend on it (e.g.,
        // when cache/visibility changes), we always copy items. This changes referential
        // identity when forceLoadData=false, but Compose uses structural equality anyway.
        _transactionItems.update { currentList ->
            if (currentList.isEmpty()) return@update currentList
            currentList.map { it.copy() }
        }
    }

    fun loadNext() {
        executorService.submit {
            transactionRecordRepository.loadNext()
        }
    }

    fun fetchRateIfNeeded(recordUid: String) {
        executorService.submit {
            _transactionItems.value.find { it.record.uid == recordUid }?.let { transactionItem ->
                if (transactionItem.currencyValue == null) {
                    transactionItem.record.mainValue?.coin?.uid?.let { coinUid ->
                        rateRepository.fetchHistoricalRate(
                            HistoricalRateKey(
                                coinUid,
                                transactionItem.record.timestamp
                            )
                        )
                    }
                }
            }
        }
    }

    fun getTransactionItem(recordUid: String): TransactionItem? {
        return _transactionItems.value.find { it.record.uid == recordUid }
    }


    override fun clear() {
        transactionRecordRepository.clear()
        rateRepository.clear()
        transactionSyncStateRepository.clear()
        coroutineScope.cancel()
        executorService.shutdown()
    }
}

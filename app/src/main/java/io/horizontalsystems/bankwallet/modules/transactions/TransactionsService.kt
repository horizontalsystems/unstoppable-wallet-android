package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class TransactionsService(
    private val transactionRecordRepository: ITransactionRecordRepository,
    private val xRateRepository: TransactionsXRateRepository,
    private val transactionSyncStateRepository: TransactionSyncStateRepository,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: IWalletManager
) : Clearable {

    private val filterCoinsSubject = BehaviorSubject.create<List<Wallet>>()
    val filterCoinsObservable: Observable<List<Wallet>> = filterCoinsSubject

    private val filterCoinSubject = BehaviorSubject.createDefault<Optional<Wallet>>(Optional.empty())
    val filterCoinObservable: Observable<Optional<Wallet>> = filterCoinSubject

    private val itemsSubject = BehaviorSubject.create<List<TransactionItem>>()
    val itemsObservable: Observable<List<TransactionItem>> get() = itemsSubject

    val syncingObservable get() = transactionSyncStateRepository.syncingObservable

    private val disposables = CompositeDisposable()
    private val transactionItems = CopyOnWriteArrayList<TransactionItem>()

    private var transactionWallets = listOf<TransactionWallet>()

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

        xRateRepository.dataExpiredObservable
            .subscribeIO {
                handleUpdatedHistoricalRates()
            }
            .let {
                disposables.add(it)
            }

        xRateRepository.historicalRateObservable
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
                if (mainValue.coin.type == key.coinType && item.record.timestamp == key.timestamp) {
                    val currencyValue = CurrencyValue(rate.currency, mainValue.value * rate.value)

                    transactionItems[i] = item.copy(currencyValue = currencyValue)
                    updated = true
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

            val currencyValue = item.record.mainValue?.let { mainValue ->
                xRateRepository.getHistoricalRate(HistoricalRateKey(mainValue.coin.type, item.record.timestamp))?.let { rate ->
                    CurrencyValue(rate.currency, mainValue.value * rate.value)
                }
            }

            transactionItems[i] = item.copy(currencyValue = currencyValue)
        }

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedRecords(transactionRecords: List<TransactionRecord>) {
        val tmpList = mutableListOf<TransactionItem>()

        transactionRecords.forEach { record ->
            val lastBlockInfo = transactionSyncStateRepository.getLastBlockInfo(record.source)
            val currencyValue = record.mainValue?.let { mainValue ->
                xRateRepository.getHistoricalRate(HistoricalRateKey(mainValue.coin.type, record.timestamp))?.let { rate ->
                    CurrencyValue(rate.currency, mainValue.value * rate.value)
                }
            }

            tmpList.add(TransactionItem(record, currencyValue, lastBlockInfo))
        }

        transactionItems.clear()
        transactionItems.addAll(tmpList)
        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        filterCoinsSubject.onNext(wallets)

        transactionWallets = wallets.map {
            TransactionWallet(it.coin, it.transactionSource)
        }

        val walletsGroupedBySource = groupWalletsBySource(transactionWallets)
        transactionSyncStateRepository.setTransactionSources(walletsGroupedBySource.map { it.source })
        transactionRecordRepository.setWallets(transactionWallets, walletsGroupedBySource)
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
                        mergedWallets.add(TransactionWallet(null, wallet.source))
                    }
                }
            }
        }
        return mergedWallets

    }

    override fun clear() {
        disposables.clear()

        transactionRecordRepository.clear()
        xRateRepository.clear()
        transactionSyncStateRepository.clear()
    }

    private val executorService = Executors.newCachedThreadPool()

    fun setFilterType(f: FilterTransactionType) {
        executorService.submit {
            transactionRecordRepository.setTransactionType(f)
        }
    }

    fun setFilterCoin(w: Wallet?) {
        executorService.submit {
            filterCoinSubject.onNext(w?.let { Optional.of(it) } ?: Optional.empty())
            transactionRecordRepository.setSelectedWallet(w?.let { TransactionWallet(it.coin, it.transactionSource) })
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
                    transactionItem.record.mainValue?.let { mainValue ->
                        xRateRepository.fetchHistoricalRate(HistoricalRateKey(mainValue.coin.type, transactionItem.record.timestamp))
                    }
                }
            }
        }
    }

    fun getTransactionItem(recordUid: String): TransactionItem? {
        return transactionItems.find { it.record.uid == recordUid }
    }
}

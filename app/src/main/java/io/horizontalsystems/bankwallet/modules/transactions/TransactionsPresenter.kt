package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil
import android.util.Log
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemCacheFactory
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class TransactionsPresenter(
        private val interactor: TransactionsModule.IInteractor,
        private val router: TransactionsModule.IRouter,
        private val viewFactory: TransactionViewItemFactory,
        private val viewCacheFactory: TransactionViewItemCacheFactory,
        private val loader: TransactionsLoader,
        private val metadataDataSource: TransactionMetadataDataSource)
    : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate, TransactionsLoader.Delegate {

    private val disposables = CompositeDisposable()
    var view: TransactionsModule.IView? = null
    private var flushSubject = PublishSubject.create<Unit>()
    private var viewItems = listOf<TransactionViewItemCache>()

    private fun updateItems(updatedItems: List<TransactionViewItemCache>) {
        viewItems = updatedItems
    }

    override fun viewDidLoad() {
        interactor.initialFetch()

        flushSubject
                .debounce(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { unit -> updateByDiffUtils() }
                .subscribe()?.let { disposables.add(it) }
    }

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction)
    }

    override fun onFilterSelect(coin: Coin?) {
        interactor.setSelectedCoinCodes(coin?.let { listOf(coin) } ?: listOf())
    }

    override fun onClear() {
        interactor.clear()
    }

    override val itemsCount: Int
        get() = viewItems.size

    override fun viewItem(index: Int): TransactionViewItemCache {
        val item = viewItems[index]
        if (item.fiatValueString == null) {
            val transactionItem = loader.itemForIndex(index)
            val coin = transactionItem.coin
            interactor.fetchRate(coin, transactionItem.record.timestamp)
        }
        return viewItems[index]
    }

    override fun itemForIndex(index: Int): TransactionViewItem {
        val transactionItem = loader.itemForIndex(index)
        val coin = transactionItem.coin
        val lastBlockHeight = metadataDataSource.getLastBlockHeight(coin)
        val threshold = metadataDataSource.getConfirmationThreshold(coin)
        val rate = metadataDataSource.getRate(coin, transactionItem.record.timestamp)

        return viewFactory.item(transactionItem, lastBlockHeight, threshold, rate)
    }

    override fun itemCacheForIndex(index: Int): TransactionViewItemCache {
        val transactionItem = loader.itemForIndex(index)
        val coin = transactionItem.coin
        val lastBlockHeight = metadataDataSource.getLastBlockHeight(coin)
        val threshold = metadataDataSource.getConfirmationThreshold(coin)
        val rate = metadataDataSource.getRate(coin, transactionItem.record.timestamp)

        return viewCacheFactory.item(transactionItem, lastBlockHeight, threshold, rate)
    }

    override fun onBottomReached() {
        loader.loadNext(false)
    }

    override fun onUpdateCoinsData(allCoinData: List<Triple<Coin, Int, Int?>>) {
        val coins = allCoinData.map { it.first }

        loader.setCoinCodes(coins)

        allCoinData.forEach { (coinCode, confirmationThreshold, lastBlockHeight) ->
            metadataDataSource.setConfirmationThreshold(confirmationThreshold, coinCode)
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockHeight(it, coinCode)
            }
        }

        loader.loadNext(true)

        val filters = when {
            coins.size < 2 -> listOf()
            else -> listOf(null).plus(coins)
        }

        view?.showFilters(filters)

        interactor.fetchLastBlockHeights()
    }

    override fun onUpdateSelectedCoinCodes(selectedCoins: List<Coin>) {
        loader.setCoinCodes(selectedCoins)
        loader.loadNext(true)
    }

    override fun didFetchRecords(records: Map<Coin, List<TransactionRecord>>) {
        loader.didFetchRecords(records)
    }

    override fun onUpdateLastBlockHeight(coin: Coin, lastBlockHeight: Int) {
        metadataDataSource.setLastBlockHeight(lastBlockHeight, coin)
        updateViewItems()
    }

    override fun onUpdateBaseCurrency() {
        metadataDataSource.clearRates()
        updateViewItems()
    }

    override fun didFetchRateNoUpdate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)
    }

    override fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)
        updateViewItems()
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, coin: Coin) {
        loader.didUpdateRecords(records, coin)
    }

    override fun onConnectionRestore() {
        updateViewItems()
    }

    //
    // TransactionsLoader Delegate
    //

    override fun onChange() {
        updateViewItems()
    }

    override fun didChangeData() {
        updateViewItems()
    }

    override fun didInsertData(fromIndex: Int, count: Int) {
        if (fromIndex == 0) {
            viewItems = listOf()
        }

        val copyOfViewItems = viewItems.toMutableList()

        Single.just(copyOfViewItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { oldItems ->
                    val tempList = mutableListOf<TransactionViewItemCache>()
                    for (i in fromIndex until fromIndex + count) {
                        tempList.add(itemCacheForIndex(i))
                    }
                    oldItems.addAll(tempList)

                    val diffCallback = TransactionViewItemDiffCallback(oldItems, viewItems)
                    val diffResult = DiffUtil.calculateDiff(diffCallback)

                    Single.just(Pair(diffResult, oldItems))
                }
                .subscribe({ (diffResult, updatedList) ->
                    if (fromIndex == 0) {
                        updateItems(updatedList)
                        view?.initialLoad()
                    } else {
                        updateItems(updatedList)
                        view?.reloadChange(diffResult)
                    }
                }, {
                    Log.e("TxPresent", "error", it)
                })?.let { disposables.add(it) }

    }

    private fun updateByDiffUtils() {
        val copyOfViewItems = viewItems.toMutableList()

        Single.just(copyOfViewItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { oldItems ->
                    val updatedItems = mutableListOf<TransactionViewItemCache>()
                    for (i in 0 until loader.itemsCount) {
                        updatedItems.add(itemCacheForIndex(i))
                    }

                    val diffCallback = TransactionViewItemDiffCallback(oldItems, updatedItems)
                    val diffResult = DiffUtil.calculateDiff(diffCallback)

                    Single.just(Pair(diffResult, updatedItems))
                }
                .subscribe({ (diffResult, updatedList) ->
                    updateItems(updatedList)
                    view?.reloadChange(diffResult)
                }, {
                    Log.e("TxPresent", "error", it)
                })?.let { disposables.add(it) }
    }

    private fun updateViewItems(){
        flushSubject.onNext(Unit)
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        interactor.fetchRecords(fetchDataList)
    }

}

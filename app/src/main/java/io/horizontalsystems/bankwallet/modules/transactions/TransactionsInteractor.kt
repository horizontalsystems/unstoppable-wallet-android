package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.NetworkAvailabilityManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val adapterManager: IAdapterManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: RateManager,
        private val networkAvailabilityManager: NetworkAvailabilityManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val ratesDisposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()
    private val transactionUpdatesDisposables = CompositeDisposable()
    private var requestedTimestamps = hashMapOf<String, Long>()

    override fun initialFetch() {
        onUpdateCoinCodes()

        adapterManager.adaptersUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateCoinCodes()
                }
                .let { disposables.add(it) }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    ratesDisposables.clear()
                    requestedTimestamps.clear()
                    delegate?.onUpdateBaseCurrency()
                }
                .let { disposables.add(it) }

        networkAvailabilityManager.networkAvailabilitySignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (networkAvailabilityManager.isConnected) {
                        delegate?.onConnectionRestore()
                    }
                }
                .let { disposables.add(it) }
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        if (fetchDataList.isEmpty()) {
            delegate?.didFetchRecords(mapOf())
            return
        }

        val flowables = mutableListOf<Single<Pair<Coin, List<TransactionRecord>>>>()

        fetchDataList.forEach { fetchData ->
            val adapter = adapterManager.adapters.find { it.coin == fetchData.coin }

            val flowable = when (adapter) {
                null -> Single.just(Pair(fetchData.coin, listOf()))
                else -> {
                    adapter.getTransactionsObservable(fetchData.hashFrom, fetchData.limit)
                            .map {
                                Pair(fetchData.coin, it)
                            }
                }
            }

            flowables.add(flowable)
        }

        Single.zip(flowables) {
            val res = mutableMapOf<Coin, List<TransactionRecord>>()
            it.forEach {
                it as Pair<Coin, List<TransactionRecord>>
                res[it.first] = it.second
            }
            res.toMap()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { records, t2 ->
                    delegate?.didFetchRecords(records)
                }
                .let { disposables.add(it) }
    }

    override fun setSelectedCoinCodes(selectedCoins: List<Coin>) {
        delegate?.onUpdateSelectedCoinCodes(if (selectedCoins.isEmpty()) adapterManager.adapters.map { it.coin } else selectedCoins)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        adapterManager.adapters.forEach { adapter ->
            adapter.lastBlockHeightUpdatedSignal
                    .throttleLast(3, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { onUpdateLastBlockHeight(adapter) }
                    .let { lastBlockHeightDisposables.add(it) }
        }
    }

    override fun fetchRate(coin: Coin, timestamp: Long) {
        val baseCurrency = currencyManager.baseCurrency
        val currencyCode = baseCurrency.code
        val composedKey = coin.code + timestamp

        if (requestedTimestamps.containsKey(composedKey)) return

        requestedTimestamps[composedKey] = timestamp

        rateManager.rateValueObservable(coin.code, currencyCode, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didFetchRate(it, coin, baseCurrency, timestamp)
                }, {
                    requestedTimestamps.remove(composedKey)
                })
                .let { ratesDisposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
        lastBlockHeightDisposables.clear()
        ratesDisposables.clear()
        transactionUpdatesDisposables.clear()
    }

    private fun onUpdateLastBlockHeight(adapter: IAdapter) {
        adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(adapter.coin, lastBlockHeight)
        }
    }

    private fun onUpdateCoinCodes() {
        transactionUpdatesDisposables.clear()

        delegate?.onUpdateCoinsData(adapterManager.adapters.map { Triple(it.coin, it.confirmationsThreshold, it.lastBlockHeight) })

        adapterManager.adapters.forEach { adapter ->
            adapter.transactionRecordsSubject
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRecords(it, adapter.coin)
                    }
                    .let { transactionUpdatesDisposables.add(it) }
        }
    }

}

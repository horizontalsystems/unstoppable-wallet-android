package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(private val adapterManager: IAdapterManager, private val currencyManager: ICurrencyManager, private val rateManager: RateManager) : TransactionsModule.IInteractor {
    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val ratesDisposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()
    private val transactionUpdatesDisposables = CompositeDisposable()
    private val requestedTimestamps = mutableMapOf<CoinCode, MutableList<Long>>()

    override fun initialFetch() {
        onUpdateCoinCodes()

        disposables.add(adapterManager.adaptersUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateCoinCodes()
                })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    ratesDisposables.clear()
                    requestedTimestamps.clear()
                    delegate?.onUpdateBaseCurrency()
                })
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        if (fetchDataList.isEmpty()) {
            delegate?.didFetchRecords(mapOf())
            return
        }

        val flowables = mutableListOf<Single<Pair<CoinCode, List<TransactionRecord>>>>()

        fetchDataList.forEach { fetchData ->
            val adapter = adapterManager.adapters.find { it.coin.code == fetchData.coinCode }

            val flowable = when (adapter) {
                null -> Single.just(Pair(fetchData.coinCode, listOf()))
                else -> {
                    adapter.getTransactionsObservable(fetchData.hashFrom, fetchData.limit)
                            .map {
                                Pair(fetchData.coinCode, it)
                            }
                }
            }

            flowables.add(flowable)
        }

        disposables.add(
                Single.zip(flowables) {
                    val res = mutableMapOf<CoinCode, List<TransactionRecord>>()
                    it.forEach {
                        it as Pair<CoinCode, List<TransactionRecord>>
                        res[it.first] = it.second
                    }
                    res.toMap()
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe { records, t2 ->
                            delegate?.didFetchRecords(records)
                        })
    }

    override fun setSelectedCoinCodes(selectedCoinCodes: List<String>) {
        delegate?.onUpdateSelectedCoinCodes(if (selectedCoinCodes.isEmpty()) adapterManager.adapters.map { it.coin.code } else selectedCoinCodes)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        adapterManager.adapters.forEach { adapter ->
            lastBlockHeightDisposables.add(adapter.lastBlockHeightUpdatedSignal
                    .throttleLast(3, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        onUpdateLastBlockHeight(adapter)
                    })
        }
    }

    override fun fetchRates(timestamps: Map<CoinCode, List<Long>>) {
        val baseCurrency = currencyManager.baseCurrency
        val currencyCode = baseCurrency.code

        timestamps.forEach {
            val coinCode = it.key
            for (timestamp in it.value) {
                if (requestedTimestamps[coinCode]?.contains(timestamp) == true) continue

                if (!requestedTimestamps.containsKey(coinCode)) {
                    requestedTimestamps[coinCode] = mutableListOf()
                }
                requestedTimestamps[coinCode]?.add(timestamp)

                ratesDisposables.add(rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.didFetchRate(it, coinCode, baseCurrency, timestamp)
                        })
            }
        }
    }

    override fun clear() {
        disposables.clear()
        lastBlockHeightDisposables.clear()
        ratesDisposables.clear()
        transactionUpdatesDisposables.clear()
    }

    private fun onUpdateLastBlockHeight(adapter: IAdapter) {
        adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(adapter.coin.code, lastBlockHeight)
        }
    }

    private fun onUpdateCoinCodes() {
        transactionUpdatesDisposables.clear()

        delegate?.onUpdateCoinsData(adapterManager.adapters.map { Triple(it.coin.code, it.confirmationsThreshold, it.lastBlockHeight) })

        adapterManager.adapters.forEach { adapter ->
            transactionUpdatesDisposables.add(adapter.transactionRecordsSubject
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRecords(it, adapter.coin.code)
                    })
        }
    }

}

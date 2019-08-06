package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.NetworkAvailabilityManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val walletManager: IWalletManager,
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

        walletManager.walletsUpdatedSignal
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
            val adapter = walletManager.wallets.find { it.coin == fetchData.coin }?.let {
                adapterManager.getAdapterForWallet(it)
            }

            val flowable = when (adapter) {
                null -> Single.just(Pair(fetchData.coin, listOf()))
                else -> {
                    adapter.getTransactions(fetchData.from, fetchData.limit)
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
        delegate?.onUpdateSelectedCoinCodes(if (selectedCoins.isEmpty()) walletManager.wallets.map { it.coin } else selectedCoins)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            adapterManager.getAdapterForWallet(wallet)?.let { adapter ->
                adapter.lastBlockHeightUpdatedFlowable
                        .throttleLast(3, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe { onUpdateLastBlockHeight(wallet, adapter) }
                        .let { lastBlockHeightDisposables.add(it) }
            }
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

    private fun onUpdateLastBlockHeight(wallet: Wallet, adapter: IAdapter) {
        adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(wallet.coin, lastBlockHeight)
        }
    }

    private fun onUpdateCoinCodes() {
        transactionUpdatesDisposables.clear()

        val coinsData: MutableList<Triple<Coin, Int, Int>> = mutableListOf()
        walletManager.wallets.forEach { wallet ->
            val adapter = adapterManager.getAdapterForWallet(wallet)
            coinsData.add(Triple(wallet.coin, adapter?.confirmationsThreshold
                    ?: 0, adapter?.lastBlockHeight ?: 0))

            adapter?.let {
                adapter.transactionRecordsFlowable
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.didUpdateRecords(it, wallet.coin)
                        }
                        .let { transactionUpdatesDisposables.add(it) }
            }
        }

        delegate?.onUpdateCoinsData(coinsData)

    }

}

package io.horizontalsystems.bankwallet.modules.transactions

import android.util.Log
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: IRateManager,
        private val connectivityManager: ConnectivityManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val ratesDisposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()
    private val transactionUpdatesDisposables = CompositeDisposable()
    private val adapterStateUpdatesDisposables = CompositeDisposable()
    private var requestedTimestamps = hashMapOf<String, Long>()

    override fun initialFetch() {
        delegate?.onUpdateWallets(walletManager.activeWallets)

        adapterManager.adaptersReadyObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    delegate?.onUpdateWallets(walletManager.activeWallets)
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

        connectivityManager.networkAvailabilitySignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (connectivityManager.isConnected) {
                        delegate?.onConnectionRestore()
                    }
                }
                .let { disposables.add(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>, initial: Boolean) {
        if (fetchDataList.isEmpty()) {
            delegate?.didFetchRecords(mapOf(), initial)
            return
        }

        val flowables: List<Single<Pair<TransactionWallet, List<TransactionRecord>>>> = fetchDataList.map { fetchData ->
            val adapter = adapterManager.getTransactionsAdapterForWallet(fetchData.wallet)

            when (adapter) {
                null -> Single.just(listOf())
                else -> adapter.getTransactionsAsync(fetchData.from, fetchData.wallet.coin, fetchData.limit)
            }.map {
                Pair(fetchData.wallet, it)
            }
        }

        Single.zip(flowables) {
            it.map {
                it as Pair<TransactionWallet, List<TransactionRecord>>
                it.first to it.second
            }.toMap()
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe ({ records ->
                    delegate?.didFetchRecords(records, initial)
                },{
                    Log.e("TransactionsInteractor", "fetchRecords error: ", it)
                })
                .let { disposables.add(it) }
    }

    override fun fetchLastBlockHeights(wallets: List<TransactionWallet>) {
        lastBlockHeightDisposables.clear()

        wallets.forEach { wallet ->
            adapterManager.getTransactionsAdapterForWallet(wallet)?.let { adapter ->
                adapter.lastBlockUpdatedFlowable
                        .throttleLast(3, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe {
                            adapter.lastBlockInfo?.let { lastBlockInfo ->
                                delegate?.onUpdateLastBlock(wallet, lastBlockInfo)
                            }
                        }
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

        rateManager.historicalRate(coin.type, currencyCode, timestamp)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe({
                    //kit returns 0 when rate is not available
                    if (it.compareTo(BigDecimal.ZERO) != 0) {
                        delegate?.didFetchRate(it, coin, baseCurrency, timestamp)
                    }
                }, {
                    requestedTimestamps.remove(composedKey)
                })
                .let { ratesDisposables.add(it) }
    }

    override fun observe(wallets: List<TransactionWallet>) {
        transactionUpdatesDisposables.clear()
        adapterStateUpdatesDisposables.clear()

        val lastBlockInfos = mutableListOf<Pair<TransactionWallet, LastBlockInfo?>>()
        val states = mutableMapOf<TransactionWallet, AdapterState>()

        wallets.forEach { wallet ->
            adapterManager.getTransactionsAdapterForWallet(wallet)?.let { adapter ->
                lastBlockInfos.add(Pair(wallet, adapter.lastBlockInfo))
                states[wallet] = adapter.transactionsState

                adapter.getTransactionRecordsFlowable(wallet.coin)
                    .subscribeIO { records ->
                        delegate?.didUpdateRecords(records, wallet)
                    }
                    .let { transactionUpdatesDisposables.add(it) }


                adapter.transactionsStateUpdatedFlowable
                    .subscribeIO {
                        delegate?.onUpdateAdapterState(adapter.transactionsState, wallet)
                    }
                    .let { adapterStateUpdatesDisposables.add(it) }
            }
        }

        delegate?.onUpdateLastBlockInfos(lastBlockInfos)
        delegate?.onUpdateAdapterStates(states)
    }

    override fun clear() {
        disposables.clear()
        lastBlockHeightDisposables.clear()
        ratesDisposables.clear()
        transactionUpdatesDisposables.clear()
        adapterStateUpdatesDisposables.clear()
    }

}

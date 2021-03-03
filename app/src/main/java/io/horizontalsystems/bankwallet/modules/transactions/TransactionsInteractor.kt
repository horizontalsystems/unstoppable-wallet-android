package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
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
        onUpdateWallets()

        adapterManager.adaptersReadyObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateWallets()
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

        val flowables: List<Single<Pair<Wallet, List<TransactionRecord>>>> = fetchDataList.map { fetchData ->
            val adapter = walletManager.wallets.find { it == fetchData.wallet }?.let {
                adapterManager.getTransactionsAdapterForWallet(it)
            }

            when (adapter) {
                null -> Single.just(listOf())
                else -> adapter.getTransactions(fetchData.from, fetchData.limit)
            }.map {
                Pair(fetchData.wallet, it)
            }
        }

        Single.zip(flowables) {
            it.map {
                it as Pair<Wallet, List<TransactionRecord>>
                it.first to it.second
            }.toMap()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { records, _ ->
                    delegate?.didFetchRecords(records, initial)
                }
                .let { disposables.add(it) }
    }

    override fun setSelectedWallets(selectedWallets: List<Wallet>) {
        delegate?.onUpdateSelectedWallets(if (selectedWallets.isEmpty()) walletManager.wallets else selectedWallets)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            adapterManager.getTransactionsAdapterForWallet(wallet)?.let { adapter ->
                adapter.lastBlockUpdatedFlowable
                        .throttleLast(3, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe { onUpdateLastBlock(wallet, adapter) }
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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

    override fun clear() {
        disposables.clear()
        lastBlockHeightDisposables.clear()
        ratesDisposables.clear()
        transactionUpdatesDisposables.clear()
        adapterStateUpdatesDisposables.clear()
    }

    private fun onUpdateLastBlock(wallet: Wallet, adapter: ITransactionsAdapter) {
        adapter.lastBlockInfo?.let { lastBlockInfo ->
            delegate?.onUpdateLastBlock(wallet, lastBlockInfo)
        }
    }

    private fun onUpdateWallets() {
        transactionUpdatesDisposables.clear()
        adapterStateUpdatesDisposables.clear()

        val walletsData = mutableListOf<Pair<Wallet, LastBlockInfo?>>()
        val adapterStates = mutableMapOf<Wallet, AdapterState>()

        walletManager.wallets.forEach { wallet ->
            adapterManager.getTransactionsAdapterForWallet(wallet)?.let { adapter ->
                walletsData.add(Pair(wallet, adapter.lastBlockInfo))
                adapterStates[wallet] = adapter.transactionsState

                adapter.transactionRecordsFlowable
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.didUpdateRecords(it, wallet)
                        }
                        .let { transactionUpdatesDisposables.add(it) }

                adapter.transactionsStateUpdatedFlowable
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.onUpdateAdapterState(adapter.transactionsState, wallet)
                        }
                        .let { adapterStateUpdatesDisposables.add(it) }
            }
        }

        delegate?.onUpdateWalletsData(walletsData)
        delegate?.initialAdapterStates(adapterStates)

    }

}

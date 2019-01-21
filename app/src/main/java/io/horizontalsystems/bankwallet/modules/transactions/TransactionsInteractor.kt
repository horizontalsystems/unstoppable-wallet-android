package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(private val walletManager: IWalletManager, private val currencyManager: ICurrencyManager, private val rateManager: RateManager) : TransactionsModule.IInteractor {
    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val ratesDisposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()
    private val transactionUpdatesDisposables = CompositeDisposable()

    override fun initialFetch() {
        onUpdateCoinCodes()

        disposables.add(walletManager.walletsUpdatedSignal
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
            val adapter = walletManager.wallets.find { it.coinCode == fetchData.coinCode }?.adapter

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
        delegate?.onUpdateSelectedCoinCodes(if (selectedCoinCodes.isEmpty()) walletManager.wallets.map { it.coinCode } else selectedCoinCodes)
    }

    override fun fetchLastBlockHeights() {
        lastBlockHeightDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            onUpdateLastBlockHeight(wallet)
            delegate?.onUpdateConfirmationThreshold(wallet.coinCode, wallet.adapter.confirmationsThreshold)

            lastBlockHeightDisposables.add(wallet.adapter.lastBlockHeightUpdatedSignal
                    .throttleLast(3, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        onUpdateLastBlockHeight(wallet)
                    })
        }
    }

    override fun fetchRates(timestamps: Map<CoinCode, List<Long>>) {
        val baseCurrency = currencyManager.baseCurrency
        val currencyCode = baseCurrency.code

        timestamps.forEach {
            val coinCode = it.key
            it.value.forEach { timestamp ->
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
    }

    private fun onUpdateLastBlockHeight(wallet: Wallet) {
        wallet.adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(wallet.coinCode, lastBlockHeight)
        }
    }

    private fun onUpdateCoinCodes() {
        delegate?.onUpdateCoinCodes(walletManager.wallets.map { it.coinCode })
        walletManager.wallets.forEach { wallet ->
            transactionUpdatesDisposables.add(wallet.adapter.transactionRecordsSubject
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        delegate?.didUpdateRecords(it, wallet.coinCode)
                    })
        }
    }

}

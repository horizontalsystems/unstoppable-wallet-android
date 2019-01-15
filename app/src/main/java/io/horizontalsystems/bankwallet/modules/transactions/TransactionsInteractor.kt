package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TransactionsInteractor(private val walletManager: IWalletManager) : TransactionsModule.IInteractor {
    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private val lastBlockHeightDisposables = CompositeDisposable()

    override fun fetchCoinCodes() {
        onUpdateCoinCodes()

        disposables.add(walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    onUpdateCoinCodes()
                })
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        if (fetchDataList.isEmpty()) {
            delegate?.didFetchRecords(mapOf())
            return
        }
//
        val flowables = mutableListOf<Flowable<Pair<CoinCode, List<TransactionRecord>>>>()

        fetchDataList.forEach { fetchData ->
            val adapter = walletManager.wallets.find { it.coinCode == fetchData.coinCode }?.adapter

            val flowable = when (adapter) {
                null -> Flowable.just(Pair(fetchData.coinCode, listOf()))
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
                Flowable.zip(flowables) {
                    val res = mutableMapOf<CoinCode, List<TransactionRecord>>()
                    it.forEach {
                        it as Pair<CoinCode, List<TransactionRecord>>
                        res[it.first] = it.second
                    }
                    res.toMap()
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe {
                            delegate?.didFetchRecords(it)
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

    override fun clear() {
        disposables.clear()
    }

    private fun onUpdateLastBlockHeight(wallet: Wallet) {
        wallet.adapter.lastBlockHeight?.let { lastBlockHeight ->
            delegate?.onUpdateLastBlockHeight(wallet.coinCode, lastBlockHeight)
        }
    }

    private fun onUpdateCoinCodes() {
        delegate?.onUpdateCoinCodes(walletManager.wallets.map { it.coinCode })
    }

}

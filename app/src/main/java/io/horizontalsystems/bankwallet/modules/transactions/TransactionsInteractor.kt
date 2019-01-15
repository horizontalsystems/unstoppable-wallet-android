package io.horizontalsystems.bankwallet.modules.transactions

import android.util.Log
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class TransactionsInteractor(private val walletManager: IWalletManager) : TransactionsModule.IInteractor {
    var delegate: TransactionsModule.IInteractorDelegate? = null

    private val disposables = CompositeDisposable()

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
        Log.e("AAA", "fetchRecords: ${fetchDataList}")
//
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

    override fun clear() {
        disposables.clear()
    }

    private fun onUpdateCoinCodes() {
        delegate?.onUpdateCoinCodes(walletManager.wallets.map { it.coinCode })
    }


//    private fun resubscribeToLastBlockHeightSubjects() {
//        lastBlockHeightDisposable?.dispose()
//        lastBlockHeightDisposable = Observable.merge(walletManager.wallets.map { it.adapter.lastBlockHeightSubject })
//                .throttleLast(3, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    delegate?.didUpdateDataSource()
//                }
//
//        lastBlockHeightDisposable?.let {
//            disposables.add(it)
//        }
//    }
}

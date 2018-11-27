package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionRateSyncer
import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class TransactionRateSyncer(
        private val storage: ITransactionRecordStorage,
        private val networkManager: INetworkManager) : ITransactionRateSyncer {

    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun sync(currencyCode: String) {
        storage.nonFilledRecords
                .subscribeOn(Schedulers.io())
                .subscribe { records ->
                    records.forEach { record ->
                        if (record.timestamp > 0L) {
                            disposables.add(networkManager.getRate(coin = record.coin, currency = currencyCode, timestamp = record.timestamp)
                                    .subscribe {
                                        storage.set(rate = it, transactionHash = record.transactionHash)
                                    })
                        }
                    }
                }.let {
                    disposables.add(it)
                }
    }

    override fun cancelCurrentSync() {
        disposables.clear()
    }
}

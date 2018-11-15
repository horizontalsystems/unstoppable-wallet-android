package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionRateSyncer
import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class TransactionRateSyncer(
        private val storage: ITransactionRecordStorage,
        private val networkManager: INetworkManager) : ITransactionRateSyncer {

    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun sync(currencyCode: String) {
        storage.nonFilledRecords.forEach { record ->
            if (record.timestamp > 0L) {
                disposables.add(networkManager.getRate(coin = record.coin, currency = currencyCode, timestamp = record.timestamp)
                                .subscribeOn(Schedulers.io())
                                .unsubscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    storage.set(rate = it, transactionHash = record.transactionHash)
                                })
            }
        }
    }

    override fun cancelCurrentSync() {
        disposables.clear()
    }
}

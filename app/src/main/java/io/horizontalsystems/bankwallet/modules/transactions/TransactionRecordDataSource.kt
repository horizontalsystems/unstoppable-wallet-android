package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class TransactionRecordDataSource(private val appDatabase: AppDatabase) : TransactionsModule.ITransactionRecordDataSource {

    private var results: List<TransactionRecord> = listOf()
    private var disposable: Disposable? = null

    override var delegate: TransactionsModule.ITransactionRecordDataSourceDelegate? = null

    override val count: Int
        get() = results.count()

    override fun recordForIndex(index: Int): TransactionRecord {
        return results[index]
    }

    override fun setCoin(coinCode: CoinCode?) {
        subscribe(coinCode)
    }

    init {
        subscribe()
    }

    private fun subscribe(coinCode: CoinCode? = null) {
        disposable?.dispose()

        disposable = getTransactionRecords(coinCode)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .unsubscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    results = it
                    delegate?.onUpdateResults()
                }
    }

    private fun getTransactionRecords(coinCode: CoinCode? = null): Flowable<List<TransactionRecord>> =
            if (coinCode == null)
                appDatabase.transactionDao().getAll()
            else
                appDatabase.transactionDao().getAll(coinCode)

}
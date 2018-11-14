package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Handler
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val walletManager: IWalletManager,
        private val dataSource: TransactionsModule.ITransactionRecordDataSource,
        private val refreshTimeout: Double = 2.0
) : TransactionsModule.IInteractor, TransactionsModule.ITransactionRecordDataSourceDelegate {

    private val disposables = CompositeDisposable()

    var delegate: TransactionsModule.IInteractorDelegate? = null

    init {
        disposables.add(Observable.merge(walletManager.wallets.map { it.adapter.lastBlockHeightSubject })
                .throttleLast(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.didUpdateDataSource()
                })
    }

    override fun retrieveFilters() {
        val coins = walletManager.wallets.map { it.coin }
        delegate?.didRetrieveFilters(coins)
    }

    override fun refresh() {
        Handler().postDelayed({
            delegate?.didRefresh()
        }, (refreshTimeout * 1000).toLong())
    }

    override fun setCoin(coin: Coin?) {
        dataSource.setCoin(coin)
    }

    override val recordsCount: Int
        get() = dataSource.count

    override fun recordForIndex(index: Int): TransactionRecord {
        return dataSource.recordForIndex(index)
    }

    override fun clear() {
        disposables.clear()
    }

    override fun onUpdateResults() {
        delegate?.didUpdateDataSource()
    }
}

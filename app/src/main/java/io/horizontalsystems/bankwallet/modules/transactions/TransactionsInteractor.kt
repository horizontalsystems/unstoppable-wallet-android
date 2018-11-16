package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Handler
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class TransactionsInteractor(
        private val walletManager: IWalletManager,
        private val dataSource: TransactionsModule.ITransactionRecordDataSource,
        private val refreshTimeout: Double = 2.0
) : TransactionsModule.IInteractor, TransactionsModule.ITransactionRecordDataSourceDelegate {

    private val disposables = CompositeDisposable()
    private var lastBlockHeightDisposable: Disposable? = null

    var delegate: TransactionsModule.IInteractorDelegate? = null

    init {
        resubscribeToLastBlockHeightSubjects(walletManager.wallets)

        disposables.add(walletManager.walletsSubject.subscribe {
            resubscribeToLastBlockHeightSubjects(it)
        })
    }

    override fun retrieveFilters() {
        delegate?.didRetrieveFilters(walletManager.wallets.map { it.coin })

        disposables.add(walletManager.walletsSubject.subscribe {
            delegate?.didRetrieveFilters(it.map { it.coin })
        })
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

    private fun resubscribeToLastBlockHeightSubjects(wallets: List<Wallet>) {
        lastBlockHeightDisposable?.dispose()
        lastBlockHeightDisposable = Observable.merge(wallets.map { it.adapter.lastBlockHeightSubject })
                .throttleLast(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.didUpdateDataSource()
                }

        lastBlockHeightDisposable?.let {
            disposables.add(it)
        }
    }
}

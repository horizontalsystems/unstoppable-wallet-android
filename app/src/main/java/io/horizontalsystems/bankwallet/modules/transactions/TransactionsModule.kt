package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
        val transactionHash: String,
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
        val from: String?,
        val to: String?,
        val incoming: Boolean,
        val date: Date?,
        val status: TransactionStatus)


sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Int) : TransactionStatus() //progress in 0..100%
    object Completed : TransactionStatus()
}

object TransactionsModule {

    interface IView {
        fun showFilters(filters: List<TransactionFilterItem>)
        fun didRefresh()
        fun reload()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun refresh()
        fun onFilterSelect(coinCode: CoinCode?)
        fun onClear()

        val itemsCount: Int
        fun itemForIndex(index: Int): TransactionViewItem
    }

    interface IInteractor {
        fun retrieveFilters()
        fun refresh()
        fun setCoin(coinCode: CoinCode?)

        val recordsCount: Int
        fun recordForIndex(index: Int): TransactionRecord
        fun clear()
    }

    interface IInteractorDelegate {
        fun didRetrieveFilters(filters: List<CoinCode>)
        fun didUpdateDataSource()
        fun didRefresh()
    }


    interface IRouter {
        fun openTransactionInfo(transactionHash: String)
    }

    interface ITransactionRecordDataSource {
        var delegate: ITransactionRecordDataSourceDelegate?

        val count: Int
        fun recordForIndex(index: Int): TransactionRecord
        fun setCoin(coinCode: CoinCode?)
    }

    interface ITransactionRecordDataSourceDelegate {
        fun onUpdateResults()
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {

        val dataSource = TransactionRecordDataSource(App.appDatabase)
        val interactor = TransactionsInteractor(App.walletManager, dataSource)
        val presenter = TransactionsPresenter(interactor, router, TransactionViewItemFactory(App.walletManager, App.currencyManager, App.rateManager))

        dataSource.delegate = interactor
        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

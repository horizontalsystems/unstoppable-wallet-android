package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.App
import bitcoin.wallet.entities.Currency

object TransactionsModule {

    interface IView {
        fun showTransactionItems(items: List<TransactionRecordViewItem>)
        fun showFilters(filters: List<TransactionFilterItem>)
        fun didRefresh()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionRecordViewItem)
        fun refresh()
        fun onFilterSelect(adapterId: String?)
        fun onCleared()
    }

    interface IInteractor {
        val baseCurrency: Currency
        fun retrieveFilters()
        fun retrieveTransactions(adapterId: String?)
        fun onCleared()
        fun refresh()
    }

    interface IInteractorDelegate {
        fun didRetrieveFilters(filters: List<TransactionFilterItem>)
        fun didRetrieveItems(items: List<TransactionRecordViewItem>)
    }

    interface IRouter {
        fun showTransactionInfo(transaction: TransactionRecordViewItem)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val interactor = TransactionsInteractor(App.adapterManager, App.exchangeRateManager, App.currencyManager)
        val presenter = TransactionsPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

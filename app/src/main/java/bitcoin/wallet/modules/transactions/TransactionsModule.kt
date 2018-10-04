package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.managers.Factory

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
        fun retrieveFilters()
        fun retrieveTransactions(adapterId: String?)
        fun onCleared()
    }

    interface IInteractorDelegate {
        fun didRetrieveFilters(filters: List<TransactionFilterItem>)
        fun didRetrieveItems(items: List<TransactionRecordViewItem>)
    }

    interface IRouter {
        fun showTransactionInfo(transaction: TransactionRecordViewItem)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val adapter = AdapterManager
        val exchangeRateManager = ExchangeRateManager
        val baseCurrencyFlowable = Factory.preferencesManager.getBaseCurrencyFlowable()
        val interactor = TransactionsInteractor(adapter, exchangeRateManager, baseCurrencyFlowable)
        val presenter = TransactionsPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.ExchangeRateManager

object TransactionsModule {

    interface IView {
        fun showTransactionItems(items: List<TransactionRecordViewItem>)
        fun showFilters(filters: List<TransactionFilterItem>)
        fun didRefresh()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionRecordViewItem, coinCode: String, txHash: String)
        fun refresh()
        fun onFilterSelect(adapterId: String?)
    }

    interface IInteractor {
        fun retrieveFilters()
        fun retrieveTransactionItems(adapterId: String? = null)
    }

    interface IInteractorDelegate {
        fun didRetrieveFilters(filters: List<TransactionFilterItem>)
        fun didRetrieveItems(items: List<TransactionRecordViewItem>)
    }

    interface IRouter {
        fun showTransactionInfo(transaction: TransactionRecordViewItem, coinCode: String, txHash: String)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val adapter = AdapterManager
        val exchangeRateManager = ExchangeRateManager
        val interactor = TransactionsInteractor(adapter, exchangeRateManager)
        val presenter = TransactionsPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

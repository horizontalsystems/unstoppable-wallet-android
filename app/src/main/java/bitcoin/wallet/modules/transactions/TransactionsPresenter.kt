package bitcoin.wallet.modules.transactions

import android.os.Handler

class TransactionsPresenter(private val interactor: TransactionsModule.IInteractor, private val router: TransactionsModule.IRouter) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    override fun viewDidLoad() {
        interactor.retrieveFilters()
    }

    override fun onTransactionItemClick(transaction: TransactionRecordViewItem) {
        router.showTransactionInfo(transaction)
    }

    override fun refresh() {
        interactor.refresh()
        Handler().postDelayed({
            view?.didRefresh()
        }, 3 * 1000)
    }

    override fun onFilterSelect(adapterId: String?) {
        println("onFilterSelect $adapterId")
        interactor.retrieveTransactions(adapterId = adapterId)
    }

    override fun didRetrieveFilters(filters: List<TransactionFilterItem>) {
        val filterItems: List<TransactionFilterItem> = filters.map { TransactionFilterItem(it.adapterId, it.name) }

        val items = filterItems.toMutableList()
        items.add(0, TransactionFilterItem(null, "All"))
        view?.showFilters(filters = items)
    }

    var view: TransactionsModule.IView? = null

    override fun didRetrieveItems(items: List<TransactionRecordViewItem>) {
        view?.showTransactionItems(items)
    }

    override fun onCleared() {
        interactor.onCleared()
    }
}

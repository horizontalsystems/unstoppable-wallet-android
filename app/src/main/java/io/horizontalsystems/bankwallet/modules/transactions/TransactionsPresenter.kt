package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory

class TransactionsPresenter(private val interactor: TransactionsModule.IInteractor, private val router: TransactionsModule.IRouter, private val factory: TransactionViewItemFactory) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.retrieveFilters()
    }

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction.transactionHash)
    }

    override fun refresh() {
        interactor.refresh()
    }

    override fun onFilterSelect(coinCode: CoinCode?) {
        interactor.setCoin(coinCode)
    }

    override fun onClear() {
        interactor.clear()
    }

    override val itemsCount: Int
        get() = interactor.recordsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        val record = interactor.recordForIndex(index)
        return factory.item(record)
    }

    override fun didRetrieveFilters(filters: List<CoinCode>) {
        val filterItems = filters.map {
            TransactionFilterItem(it, it)
        }.toMutableList()
        filterItems.add(0, TransactionFilterItem(null, "All"))
        view?.showFilters(filterItems)
    }

    override fun didUpdateDataSource() {
        view?.reload()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    //    override fun viewDidLoad() {
//        interactor.retrieveFilters()
//    }
//
//    override fun onTransactionItemClick(transaction: TransactionRecordViewItem) {
//        router.showTransactionInfo(transaction)
//    }
//
//    override fun refresh() {
//        interactor.refresh()
//        Handler().postDelayed({
//            view?.didRefresh()
//        }, 3 * 1000)
//    }
//
//    override fun onFilterSelect(adapterId: String?) {
//        println("onFilterSelect $adapterId")
//        interactor.retrieveTransactions(adapterId = adapterId)
//    }
//
//    override fun didRetrieveFilters(filters: List<TransactionFilterItem>) {
//        val filterItems: List<TransactionFilterItem> = filters.map { TransactionFilterItem(it.adapterId, it.name) }
//
//        val items = filterItems.toMutableList()
//        items.add(0, TransactionFilterItem(null, "All"))
//        view?.showFilters(filters = items)
//    }
//
//    var view: TransactionsModule.IView? = null
//
//    override fun didRetrieveItems(items: List<TransactionRecordViewItem>) {
//        view?.showTransactionItems(items)
//    }
//
//    override fun onClear() {
//        interactor.onCleared()
//    }
}

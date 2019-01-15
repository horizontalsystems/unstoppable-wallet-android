package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionsPresenter(private val interactor: TransactionsModule.IInteractor, private val router: TransactionsModule.IRouter, private val factory: TransactionViewItemFactory, private val loader: TransactionsLoader) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate, TransactionsLoader.Delegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.fetchCoinCodes()
    }

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction.transactionHash)
    }

    override fun onFilterSelect(coinCode: CoinCode?) {
        interactor.setSelectedCoinCodes(coinCode?.let { listOf(coinCode) } ?: listOf())
    }

    override fun onClear() {
        interactor.clear()
    }

    override val itemsCount: Int
        get() = loader.itemsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        return factory.item(loader.itemForIndex(index))
    }

    override fun onBottomReached() {
        if (!loader.loading) {
            loader.loadNext()
        }
    }

    override fun onUpdateCoinCodes(allCoinCodes: List<CoinCode>) {
        loader.setCoinCodes(allCoinCodes)
        loader.loading = false
        loader.loadNext()

        view?.showFilters(listOf(null).plus(allCoinCodes))
    }

    override fun onUpdateSelectedCoinCodes(selectedCoinCodes: List<CoinCode>) {
        loader.setCoinCodes(selectedCoinCodes)
        loader.loading = false
        loader.loadNext()
    }

    override fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        loader.didFetchRecords(records)
    }

    override fun didChangeData() {
        loader.loading = false
        view?.reload()
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        interactor.fetchRecords(fetchDataList)
    }
}

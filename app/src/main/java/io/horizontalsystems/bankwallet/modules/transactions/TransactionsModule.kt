package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
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

    data class FetchData(val coinCode: CoinCode, val hashFrom: String?, val limit: Int)

    interface IView {
        fun showFilters(filters: List<CoinCode?>)
        fun reload(fromIndex: Int? = null, count: Int? = null)
        fun reloadItems(indexes: List<Int>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun onFilterSelect(coinCode: CoinCode?)
        fun onClear()

        val itemsCount: Int
        fun itemForIndex(index: Int): TransactionViewItem
        fun onBottomReached()
    }

    interface IInteractor {
        fun initialFetch()
        fun clear()
        fun fetchRecords(fetchDataList: List<FetchData>)
        fun setSelectedCoinCodes(selectedCoinCodes: List<CoinCode>)
        fun fetchLastBlockHeights()
        fun fetchRates(timestamps: Map<CoinCode, List<Long>>)
    }

    interface IInteractorDelegate {
        fun onUpdateCoinCodes(allCoinCodes: List<CoinCode>)
        fun onUpdateSelectedCoinCodes(selectedCoinCodes: List<CoinCode>)
        fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>)
        fun onUpdateLastBlockHeight(coinCode: CoinCode, lastBlockHeight: Int)
        fun onUpdateConfirmationThreshold(coinCode: CoinCode, confirmationThreshold: Int)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: Double, coinCode: CoinCode, currency: Currency, timestamp: Long)
    }

    interface IRouter {
        fun openTransactionInfo(transactionHash: String)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val dataSource = TransactionRecordDataSource()
        val interactor = TransactionsInteractor(App.walletManager, App.currencyManager, App.rateManager)
        val transactionsLoader = TransactionsLoader(dataSource)
        val presenter = TransactionsPresenter(interactor, router, TransactionViewItemFactory(App.walletManager, App.currencyManager, App.rateManager), transactionsLoader, TransactionMetadataDataSource())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
        transactionsLoader.delegate = presenter
    }

}

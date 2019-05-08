package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
        val transactionHash: String,
        val coin: Coin,
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
        val from: String?,
        val to: String?,
        val incoming: Boolean,
        val date: Date?,
        val status: TransactionStatus,
        val rate: CurrencyValue?)


sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Int) : TransactionStatus() //progress in 0..100%
    object Completed : TransactionStatus()
}

object TransactionsModule {

    data class FetchData(val coin: Coin, val from: Pair<String, Int>?, val limit: Int)

    interface IView {
        fun showFilters(filters: List<Coin?>)
        fun reload()
        fun reloadChange(diff: DiffUtil.DiffResult)
        fun reloadItems(updatedIndexes: List<Int>)
        fun addItems(fromIndex: Int, count: Int)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun onFilterSelect(coin: Coin?)
        fun onClear()

        val itemsCount: Int
        fun itemForIndex(index: Int): TransactionViewItem
        fun onBottomReached()
        fun onVisible()
    }

    interface IInteractor {
        fun initialFetch()
        fun clear()
        fun fetchRecords(fetchDataList: List<FetchData>)
        fun setSelectedCoinCodes(selectedCoins: List<Coin>)
        fun fetchLastBlockHeights()
        fun fetchRate(coin: Coin, timestamp: Long)
    }

    interface IInteractorDelegate {
        fun onUpdateCoinsData(allCoinData: List<Triple<Coin, Int, Int?>>)
        fun onUpdateSelectedCoinCodes(selectedCoinCodes: List<Coin>)
        fun didFetchRecords(records: Map<Coin, List<TransactionRecord>>)
        fun onUpdateLastBlockHeight(coin: Coin, lastBlockHeight: Int)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, coin: Coin)
        fun onConnectionRestore()
    }

    interface IRouter {
        fun openTransactionInfo(transactionViewItem: TransactionViewItem)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), TransactionItemFactory())
        val interactor = TransactionsInteractor(App.adapterManager, App.currencyManager, App.rateManager, App.networkAvailabilityManager)
        val transactionsLoader = TransactionsLoader(dataSource)
        val presenter = TransactionsPresenter(interactor, router, TransactionViewItemFactory(), transactionsLoader, TransactionMetadataDataSource())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
        transactionsLoader.delegate = presenter
    }

}

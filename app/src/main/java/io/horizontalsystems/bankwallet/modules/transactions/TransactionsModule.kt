package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
        val wallet: Wallet,
        val transactionHash: String,
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue?,
        val feeCoinValue: CoinValue?,
        val from: String?,
        val to: String?,
        val type: TransactionType,
        val showFromAddress: Boolean,
        val date: Date?,
        val status: TransactionStatus,
        val rate: CurrencyValue?,
        val lockInfo: TransactionLockInfo?,
        val conflictingTxHash: String?,
        val unlocked: Boolean = true,
        val record: TransactionRecord) : Comparable<TransactionViewItem> {

    override fun compareTo(other: TransactionViewItem): Int {
        return record.compareTo(other.record)
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionViewItem) {
            return record == other.record
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return record.hashCode()
    }
}


data class TransactionLockInfo(val lockedUntil: Date, val originalAddress: String, val amount: BigDecimal?)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Double) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

object TransactionsModule {

    data class FetchData(val wallet: Wallet, val from: TransactionRecord?, val limit: Int)

    interface IView {
        fun showFilters(filters: List<Wallet?>)
        fun reload()
        fun reloadChange(diff: DiffUtil.DiffResult)
        fun reloadItems(updatedIndexes: List<Int>)
        fun addItems(fromIndex: Int, count: Int)
        fun showNoTransactions()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun onFilterSelect(wallet: Wallet?)
        fun onClear()

        val itemsCount: Int
        fun itemForIndex(index: Int): TransactionViewItem
        fun onBottomReached()
        fun onVisible()
    }

    interface IInteractor {
        fun initialFetch()
        fun clear()
        fun fetchRecords(fetchDataList: List<FetchData>, initial: Boolean)
        fun setSelectedWallets(selectedWallets: List<Wallet>)
        fun fetchLastBlockHeights()
        fun fetchRate(coin: Coin, timestamp: Long)
    }

    interface IInteractorDelegate {
        fun onUpdateWalletsData(allWalletsData: List<Triple<Wallet, Int, LastBlockInfo?>>)
        fun onUpdateSelectedWallets(selectedWallets: List<Wallet>)
        fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>, initial: Boolean)
        fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet)
        fun onConnectionRestore()
    }

    interface IRouter {
        fun openTransactionInfo(transactionViewItem: TransactionViewItem)
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val metadataDataSource = TransactionMetadataDataSource()
        val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 10, TransactionViewItemFactory(App.feeCoinProvider), metadataDataSource)
        val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
        val presenter = TransactionsPresenter(interactor, router, TransactionViewItemFactory(App.feeCoinProvider), dataSource, metadataDataSource)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

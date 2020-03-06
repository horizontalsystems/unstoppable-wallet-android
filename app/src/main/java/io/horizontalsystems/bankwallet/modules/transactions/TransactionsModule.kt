package io.horizontalsystems.bankwallet.modules.transactions

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
        var currencyValue: CurrencyValue?,
        val feeCoinValue: CoinValue?,
        val from: String?,
        val to: String?,
        val type: TransactionType,
        val showFromAddress: Boolean,
        val date: Date?,
        val status: TransactionStatus,
        var rate: CurrencyValue?,
        val lockInfo: TransactionLockInfo?,
        val conflictingTxHash: String?,
        val unlocked: Boolean = true,
        val record: TransactionRecord) : Comparable<TransactionViewItem> {

    val isPending: Boolean
        get() = status is TransactionStatus.Pending || status is TransactionStatus.Processing

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

    fun itemTheSame(other: TransactionViewItem): Boolean {
        return record == other.record
    }

    fun contentTheSame(other: TransactionViewItem): Boolean {
        return currencyValue == other.currencyValue
                && date == other.date
                && status == other.status
                && rate == other.rate
                && unlocked == other.unlocked
                && conflictingTxHash == other.conflictingTxHash
    }

    fun clearRates() {
        currencyValue = null
        rate = null
    }

    fun becomesUnlocked(oldBlockTimestamp: Long?, lastBlockTimestamp: Long?): Boolean {
        if (lastBlockTimestamp == null || record.lockInfo == null) return false

        val lockedUntilTimestamp = record.lockInfo.lockedUntil.time / 1000

        return lockedUntilTimestamp > oldBlockTimestamp ?: 0 && lockedUntilTimestamp <= lastBlockTimestamp
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
        fun showTransactions(items: List<TransactionViewItem>)
        fun showNoTransactions()
        fun reloadTransactions()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTransactionItemClick(transaction: TransactionViewItem)
        fun onFilterSelect(wallet: Wallet?)
        fun onClear()

        fun onBottomReached()
        fun willShow(transactionViewItem: TransactionViewItem)
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
        val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 10, TransactionViewItemFactory(App.feeCoinProvider), TransactionMetadataDataSource())
        val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
        val presenter = TransactionsPresenter(interactor, router, dataSource)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}

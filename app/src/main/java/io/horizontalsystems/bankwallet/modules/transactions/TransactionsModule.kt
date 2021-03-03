package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionLockState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
        val wallet: Wallet,
        val record: TransactionRecord,
        val coinValue: CoinValue,
        var currencyValue: CurrencyValue?,
        val type: TransactionType,
        val date: Date?,
        val status: TransactionStatus,
        val lockState: TransactionLockState?,
        val doubleSpend: Boolean) : Comparable<TransactionViewItem> {

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
                && lockState == other.lockState
                && doubleSpend == other.doubleSpend
    }

    fun clearRates() {
        currencyValue = null
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
        fun showSyncing()
        fun hideSyncing()
        fun showFilters(filters: List<Wallet?>)
        fun showTransactions(items: List<TransactionViewItem>)
        fun showNoTransactions()
        fun reloadTransactions()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onFilterSelect(wallet: Wallet?)
        fun onClear()

        fun onBottomReached()
        fun willShow(transactionViewItem: TransactionViewItem)
        fun showDetails(item: TransactionViewItem)
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
        fun onUpdateWalletsData(allWalletsData: List<Pair<Wallet, LastBlockInfo?>>)
        fun onUpdateSelectedWallets(selectedWallets: List<Wallet>)
        fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>, initial: Boolean)
        fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet)
        fun onConnectionRestore()
        fun initialAdapterStates(states: Map<Wallet, AdapterState>)
        fun onUpdateAdapterState(state: AdapterState, wallet: Wallet)
    }

    interface IRouter {
    }

    fun initModule(view: TransactionsViewModel, router: IRouter) {
        val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 10, TransactionViewItemFactory(), TransactionMetadataDataSource())
        val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
        val presenter = TransactionsPresenter(interactor, router, dataSource)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewAndRouter = TransactionsViewModel()

            val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 10, TransactionViewItemFactory(), TransactionMetadataDataSource())
            val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
            val presenter = TransactionsPresenter(interactor, viewAndRouter, dataSource)

            presenter.view = viewAndRouter
            interactor.delegate = presenter
            viewAndRouter.delegate = presenter

            presenter.viewDidLoad()

            return viewAndRouter as T
        }
    }


}

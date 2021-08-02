package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
    val wallet: TransactionWallet,
    val record: TransactionRecord,
    val type: TransactionType,
    val date: Date?,
    val status: TransactionStatus,
    var mainAmountCurrencyString: String?
) : Comparable<TransactionViewItem> {

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
        return mainAmountCurrencyString == other.mainAmountCurrencyString
                && date == other.date
                && status == other.status
                && type == other.type
    }

    fun clearRates() {
        mainAmountCurrencyString = null
    }
}

data class TransactionLockInfo(
    val lockedUntil: Date,
    val originalAddress: String,
    val amount: BigDecimal?
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Double) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

data class TransactionWallet(
    val coin: Coin?,
    val source: TransactionSource
)

data class TransactionSource(
    val blockchain: Blockchain,
    val account: Account,
    val coinSettings: CoinSettings
) {

    sealed class Blockchain {
        object Bitcoin : Blockchain()
        object Litecoin : Blockchain()
        object BitcoinCash : Blockchain()
        object Dash : Blockchain()
        object Ethereum : Blockchain()
        object Zcash : Blockchain()
        object BinanceSmartChain : Blockchain()
        class Bep2(val symbol: String) : Blockchain(){
            override fun hashCode(): Int {
                return this.symbol.hashCode()
            }
            override fun equals(other: Any?): Boolean {
                return when(other){
                    is Bep2 -> this.symbol == other.symbol
                    else -> false
                }
            }
        }

        fun getTitle(): String {
            return when(this){
                Bitcoin -> "Bitcoin"
                Litecoin -> "Litecoin"
                BitcoinCash -> "BitcoinCash"
                Dash -> "Dash"
                Ethereum -> "Ethereum"
                Zcash -> "Zcash"
                BinanceSmartChain -> "Binance Smart Chain"
                is Bep2 -> "Binance Chain"
            }
        }
    }

}

object TransactionsModule {

    data class FetchData(val wallet: TransactionWallet, val from: TransactionRecord?, val limit: Int)

    interface IView {
        fun showSyncing()
        fun hideSyncing()
        fun showFilters(filters: List<Wallet?>, selectedFilter: Wallet?)
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
        fun fetchLastBlockHeights(transactionWallets: List<TransactionWallet>)
        fun fetchRate(coin: Coin, timestamp: Long)
        fun observe(wallets: List<TransactionWallet>)
    }

    interface IInteractorDelegate {
        fun didFetchRecords(records: Map<TransactionWallet, List<TransactionRecord>>, initial: Boolean)
        fun onUpdateLastBlock(wallet: TransactionWallet, lastBlockInfo: LastBlockInfo)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, wallet: TransactionWallet)
        fun onConnectionRestore()
        fun onUpdateAdapterStates(states: Map<TransactionWallet, AdapterState>)
        fun onUpdateAdapterState(state: AdapterState, wallet: TransactionWallet)
        fun onUpdateWallets(wallets: List<Wallet>)
        fun onUpdateLastBlockInfos(lastBlockInfos: MutableList<Pair<TransactionWallet, LastBlockInfo?>>)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = TransactionsViewModel()

            val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 20, TransactionViewItemFactory(
                TransactionInfoAddressMapper, App.numberFormatter
            ), TransactionMetadataDataSource())
            val interactor = TransactionsInteractor(App.walletManager, App.adapterManager, App.currencyManager, App.xRateManager, App.connectivityManager)
            val presenter = TransactionsPresenter(interactor, dataSource)

            presenter.view = view
            interactor.delegate = presenter
            view.delegate = presenter

            presenter.viewDidLoad()

            return view as T
        }
    }


}

package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource.*
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType.*
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionsPresenter(
    private val interactor: TransactionsModule.IInteractor,
    private val dataSource: TransactionRecordDataSource
) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null
    var itemDetails: TransactionViewItem? = null
    var wallets = emptyList<Wallet>()

    val allTransactionWallets: List<TransactionWallet>
        get() {
            val transactionWallets = wallets.map { transactionWallet(it) }
            val mergedWallets = mutableListOf<TransactionWallet>()

            transactionWallets.forEach { wallet ->
                when (wallet.source.blockchain) {
                    Blockchain.Bitcoin,
                    Blockchain.BitcoinCash,
                    Blockchain.Litecoin,
                    Blockchain.Dash,
                    Blockchain.Zcash,
                    is Blockchain.Bep2 -> mergedWallets.add(wallet)
                    Blockchain.Ethereum,
                    Blockchain.BinanceSmartChain -> {
                        if (mergedWallets.firstOrNull { it.source == wallet.source } == null) {
                            mergedWallets.add(TransactionWallet(null, wallet.source))
                        }
                    }
                }
            }
            return mergedWallets
        }

    private var adapterStates: MutableMap<TransactionWallet, AdapterState> = mutableMapOf()
    private var loading: Boolean = false

    override fun viewDidLoad() {
        interactor.initialFetch()
    }

    override fun onFilterSelect(wallet: Wallet?) {
        val selectedWallets: List<TransactionWallet> = when (wallet) {
            null -> allTransactionWallets
            else -> listOf(transactionWallet(wallet))
        }
        dataSource.setWallets(selectedWallets)
        loadNext(true)
    }

    override fun onClear() {
        interactor.clear()
    }

    override fun onBottomReached() {
        loadNext(false)
    }

    override fun willShow(transactionViewItem: TransactionViewItem) {
        val coin = transactionViewItem.record.mainValue?.coin ?: return
        val date = transactionViewItem.date ?: return
        if (transactionViewItem.mainAmountCurrencyString == null) {
            interactor.fetchRate(coin, date.time / 1000)
        }
    }

    override fun showDetails(item: TransactionViewItem) {
        itemDetails = item
    }

    override fun didFetchRecords(
        records: Map<TransactionWallet, List<TransactionRecord>>,
        initial: Boolean
    ) {
        dataSource.handleNextRecords(records)
        if (dataSource.increasePage()) {
            view?.showTransactions(dataSource.itemsCopy)
        } else if (initial) {
            view?.showNoTransactions()
        }
        loading = false
    }

    override fun onUpdateLastBlock(wallet: TransactionWallet, lastBlockInfo: LastBlockInfo) {
        if (dataSource.setLastBlock(wallet, lastBlockInfo)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun onUpdateBaseCurrency() {
        dataSource.clearRates()
        view?.showTransactions(dataSource.itemsCopy)
    }

    override fun didFetchRate(
        rateValue: BigDecimal,
        coin: Coin,
        currency: Currency,
        timestamp: Long
    ) {
        if (dataSource.setRate(rateValue, coin, currency, timestamp)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, wallet: TransactionWallet) {
        if (dataSource.handleUpdatedRecords(records, wallet)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun onConnectionRestore() {
        view?.reloadTransactions()
    }

    override fun onUpdateAdapterStates(states: Map<TransactionWallet, AdapterState>) {
        adapterStates = states.toMutableMap()
        syncState()
    }

    override fun onUpdateAdapterState(state: AdapterState, wallet: TransactionWallet) {
        adapterStates[wallet] = state
        syncState()
    }

    override fun onUpdateWallets(wallets: List<Wallet>) {
        this.wallets = wallets.sortedBy { it.coin.code }
        val filters = when {
            wallets.size < 2 -> listOf()
            else -> listOf(null).plus(this.wallets)
        }

        view?.showFilters(filters)

        val transactionWallets = allTransactionWallets
        dataSource.handleUpdatedWallets(transactionWallets)
        interactor.fetchLastBlockHeights(transactionWallets)

        interactor.observe(transactionWallets)
    }

    override fun onUpdateLastBlockInfos(lastBlockInfos: MutableList<Pair<TransactionWallet, LastBlockInfo?>>) {
        dataSource.handleUpdatedLastBlockInfos(lastBlockInfos)
        loadNext(true)
    }

    private fun syncState() {
        if (adapterStates.any { it.value is AdapterState.Syncing }) {
            view?.showSyncing()
        } else {
            view?.hideSyncing()
        }
    }

    private fun loadNext(initial: Boolean = false) {
        if (loading) return
        loading = true

        if (dataSource.allShown) {
            if (initial) {
                view?.showNoTransactions()
            }
            loading = false
            return
        }

        interactor.fetchRecords(dataSource.getFetchDataList(), initial)
    }

    private fun transactionWallet(wallet: Wallet): TransactionWallet {
        val coinSettings = wallet.configuredCoin.settings
        val coin = wallet.coin

        return when (val type = coin.type) {
            Bitcoin -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Bitcoin, wallet.account, coinSettings)
            )
            BitcoinCash -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.BitcoinCash, wallet.account, coinSettings)
            )
            Dash -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Dash, wallet.account, coinSettings)
            )
            Litecoin -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Litecoin, wallet.account, coinSettings)
            )
            Ethereum -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Ethereum, wallet.account, coinSettings)
            )
            BinanceSmartChain -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.BinanceSmartChain, wallet.account, coinSettings)
            )
            Zcash -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Zcash, wallet.account, coinSettings)
            )
            is Bep2 -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Bep2(type.symbol), wallet.account, coinSettings)
            )
            is Erc20 -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.Ethereum, wallet.account, coinSettings)
            )
            is Bep20 -> TransactionWallet(
                coin,
                TransactionSource(Blockchain.BinanceSmartChain, wallet.account, coinSettings)
            )
            is Unsupported -> throw IllegalArgumentException("Unsupported coin may not have transactions to show")
        }
    }

}

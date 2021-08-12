package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: IAdapterManager,
) : Clearable {

    private var selectedWallet: Wallet? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    private val items = CopyOnWriteArrayList<TransactionRecord>()
    private val loading = AtomicBoolean(false)
    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapperXxx>()

    private val disposables = CompositeDisposable()

    private var wallets = emptyList<Wallet>()
    private val allTransactionWallets: List<TransactionWallet>
        get() {
            val transactionWallets = wallets.map { transactionWallet(it) }
            val mergedWallets = mutableListOf<TransactionWallet>()

            transactionWallets.forEach { wallet ->
                when (wallet.source.blockchain) {
                    TransactionSource.Blockchain.Bitcoin,
                    TransactionSource.Blockchain.BitcoinCash,
                    TransactionSource.Blockchain.Litecoin,
                    TransactionSource.Blockchain.Dash,
                    TransactionSource.Blockchain.Zcash,
                    is TransactionSource.Blockchain.Bep2 -> mergedWallets.add(wallet)
                    TransactionSource.Blockchain.Ethereum,
                    TransactionSource.Blockchain.BinanceSmartChain -> {
                        if (mergedWallets.none { it.source == wallet.source }) {
                            mergedWallets.add(TransactionWallet(null, wallet.source))
                        }
                    }
                }
            }
            return mergedWallets
        }

    fun setWallets(wallets: List<Wallet>) {
        this.wallets = wallets

        val transactionWallets = wallets.map { transactionWallet(it) }.toMutableList()

        getAllEthTransactionWallet(wallets)?.let {
            transactionWallets.add(it)
        }

        getAllBscTransactionWallet(wallets)?.let {
            transactionWallets.add(it)
        }

        transactionWallets.forEach { transactionWallet ->
            if (adaptersMap[transactionWallet] == null) {
                adapterManager.getTransactionsAdapterForWallet(transactionWallet)?.let {
                    val transactionAdapterWrapperXxx = TransactionAdapterWrapperXxx(it, transactionWallet)
                    adaptersMap[transactionWallet] = transactionAdapterWrapperXxx
                }
            }
        }


        items.clear()
        adaptersMap.forEach { t, u ->
            u.markUsed(null)
        }
        loadNext()
    }

    private fun getAllEthTransactionWallet(wallets: List<Wallet>): TransactionWallet? {
        return wallets.firstOrNull {
            it.coin.type is CoinType.Ethereum || it.coin.type is CoinType.Erc20
        }?.let { wallet ->
            TransactionWallet(
                null,
                TransactionSource(
                    TransactionSource.Blockchain.Ethereum,
                    wallet.account,
                    wallet.configuredCoin.settings
                )
            )
        }
    }

    private fun getAllBscTransactionWallet(wallets: List<Wallet>): TransactionWallet? {
        return wallets.firstOrNull {
            it.coin.type is CoinType.BinanceSmartChain || it.coin.type is CoinType.Bep20
        }?.let { wallet ->
            TransactionWallet(
                null,
                TransactionSource(
                    TransactionSource.Blockchain.BinanceSmartChain,
                    wallet.account,
                    wallet.configuredCoin.settings
                )
            )
        }
    }

    fun setSelectedWallet(wallet: Wallet?) {
        selectedWallet = wallet

        items.clear()
        adaptersMap.forEach { t, u ->
            u.markUsed(null)
        }
        loadNext()
    }

    @Synchronized
    fun loadNext() {
        if (loading.get()) return
        loading.set(true)

        val activeWallets: List<TransactionWallet> =
            when (val selectedWalletTmp = selectedWallet) {
                null -> allTransactionWallets
                else -> listOf(transactionWallet(selectedWalletTmp))
            }

        val map: List<Single<List<Pair<TransactionWallet, TransactionRecord>>>> = activeWallets.mapNotNull { transactionWallet ->
            adaptersMap[transactionWallet]?.let { transactionAdapterWrapperXxx ->
                transactionAdapterWrapperXxx
                    .getNext(itemsPerPage)
                    .map { transactionRecords: List<TransactionRecord> ->
                        transactionRecords.map {
                            Pair(transactionWallet, it)
                        }
                    }
            }
        }

        Single
            .zip(map) {
                it as Array<List<Pair<TransactionWallet, TransactionRecord>>>
                it.toList().flatten()
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .doFinally {
                loading.set(false)
            }
            .subscribe { records ->
                handleRecords(records)
            }
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        disposables.clear()
    }

    private fun handleRecords(records: List<Pair<TransactionWallet, TransactionRecord>>) {
        if (records.isEmpty()) return

        records
            .sortedByDescending { it.second }
            .take(itemsPerPage)
            .forEach {
                adaptersMap[it.first]?.markUsed(it.second)

                items.add(it.second)
            }

        itemsSubject.onNext(items)
    }

    private fun transactionWallet(wallet: Wallet): TransactionWallet {
        val coinSettings = wallet.configuredCoin.settings
        val coin = wallet.coin

        return when (val type = coin.type) {
            CoinType.Bitcoin -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.Bitcoin,
                    wallet.account,
                    coinSettings
                )
            )
            CoinType.BitcoinCash -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.BitcoinCash,
                    wallet.account,
                    coinSettings
                )
            )
            CoinType.Dash -> TransactionWallet(
                coin,
                TransactionSource(TransactionSource.Blockchain.Dash, wallet.account, coinSettings)
            )
            CoinType.Litecoin -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.Litecoin,
                    wallet.account,
                    coinSettings
                )
            )
            CoinType.Ethereum -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.Ethereum,
                    wallet.account,
                    coinSettings
                )
            )
            CoinType.BinanceSmartChain -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.BinanceSmartChain,
                    wallet.account,
                    coinSettings
                )
            )
            CoinType.Zcash -> TransactionWallet(
                coin,
                TransactionSource(TransactionSource.Blockchain.Zcash, wallet.account, coinSettings)
            )
            is CoinType.Bep2 -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.Bep2(type.symbol),
                    wallet.account,
                    coinSettings
                )
            )
            is CoinType.Erc20 -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.Ethereum,
                    wallet.account,
                    coinSettings
                )
            )
            is CoinType.Bep20 -> TransactionWallet(
                coin,
                TransactionSource(
                    TransactionSource.Blockchain.BinanceSmartChain,
                    wallet.account,
                    coinSettings
                )
            )
            is CoinType.Unsupported -> throw IllegalArgumentException("Unsupported coin may not have transactions to show")
        }
    }

    companion object {
        const val itemsPerPage = 20
    }

}
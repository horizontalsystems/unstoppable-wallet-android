package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.ConcurrentHashMap

class TransactionAdapterManager(
    private val adapterManager: IAdapterManager,
    private val adapterFactory: AdapterFactory
) {
    private val disposables = CompositeDisposable()

    private val adaptersReadySubject = BehaviorSubject.create<Unit>()
    val adaptersReadyObservable: Observable<Unit> get() = adaptersReadySubject

    private val adaptersMap = ConcurrentHashMap<TransactionSource, ITransactionsAdapter>()

    init {
        adapterManager.adaptersReadyObservable
            .subscribeIO {
                initAdapters(it)
            }
            .let {
                disposables.add(it)
            }
    }

    fun getAdapter(source: TransactionSource): ITransactionsAdapter? = adaptersMap[source]

    private fun evmTransactionAdapter(wallet: Wallet, blockchain: TransactionSource.Blockchain) =
        when (wallet.coinType) {
            CoinType.Ethereum, is CoinType.Erc20 -> {
                if (blockchain == TransactionSource.Blockchain.Ethereum) {
                    adapterFactory.ethereumTransactionsAdapter(wallet.transactionSource)
                } else {
                    null
                }
            }
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> {
                if (blockchain == TransactionSource.Blockchain.BinanceSmartChain) {
                    adapterFactory.bscTransactionsAdapter(wallet.transactionSource)
                } else {
                    null
                }
            }
            else -> null
        }


    private fun initAdapters(adaptersMap: Map<Wallet, IAdapter>) {
        val newAdapterMap = mutableMapOf<TransactionSource, ITransactionsAdapter>()

        for ((wallet, adapter) in adaptersMap) {
            val source = wallet.transactionSource
            if (newAdapterMap.containsKey(source)) continue

            val transactionsAdapter = when (source.blockchain) {
                TransactionSource.Blockchain.Ethereum,
                TransactionSource.Blockchain.BinanceSmartChain -> {
                    evmTransactionAdapter(wallet, source.blockchain)
                }
                else -> adapter as? ITransactionsAdapter
            }

            transactionsAdapter?.let {
                newAdapterMap[source] = transactionsAdapter
            }
        }

        this.adaptersMap.forEach {
            adapterFactory.unlinkAdapter(it.key)
        }
        this.adaptersMap.clear()
        this.adaptersMap.putAll(newAdapterMap)

        adaptersReadySubject.onNext(Unit)

    }
}

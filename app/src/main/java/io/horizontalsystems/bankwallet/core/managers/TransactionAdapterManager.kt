package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class TransactionAdapterManager(
    private val adapterManager: IAdapterManager,
    private val adapterFactory: AdapterFactory
) {
    private val disposables = CompositeDisposable()

    private val _adaptersReadyFlow = MutableStateFlow(mapOf<TransactionSource, ITransactionsAdapter>())
    val adaptersReadyFlow get() = _adaptersReadyFlow.asStateFlow()

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

    private fun initAdapters(adaptersMap: Map<Wallet, IAdapter>) {
        val currentAdapters = this.adaptersMap.toMutableMap()
        this.adaptersMap.clear()

        for ((wallet, adapter) in adaptersMap) {
            val source = wallet.transactionSource
            if (this.adaptersMap.containsKey(source)) continue

            var txAdapter = currentAdapters.remove(source)
            if (txAdapter == null) {
                txAdapter = when (val blockchainType = source.blockchain.type) {
                    BlockchainType.Ethereum,
                    BlockchainType.BinanceSmartChain,
                    BlockchainType.Polygon,
                    BlockchainType.Avalanche,
                    BlockchainType.Optimism,
                    BlockchainType.Gnosis,
                    BlockchainType.Fantom,
                    BlockchainType.ArbitrumOne -> {
                        adapterFactory.evmTransactionsAdapter(wallet.transactionSource, blockchainType)
                    }
                    BlockchainType.Solana -> {
                        adapterFactory.solanaTransactionsAdapter(wallet.transactionSource)
                    }
                    BlockchainType.Tron -> {
                        adapterFactory.tronTransactionsAdapter(wallet.transactionSource)
                    }
                    else -> adapter as? ITransactionsAdapter
                }
            }

            txAdapter?.let {
                this.adaptersMap[source] = it
            }
        }

        currentAdapters.forEach {
            adapterFactory.unlinkAdapter(it.key)
        }

        _adaptersReadyFlow.update {
            this.adaptersMap
        }
    }
}

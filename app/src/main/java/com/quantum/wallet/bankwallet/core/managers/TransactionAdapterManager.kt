package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.IAdapter
import com.quantum.wallet.bankwallet.core.IAdapterManager
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.factories.AdapterFactory
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.util.concurrent.ConcurrentHashMap

class TransactionAdapterManager(
    private val adapterManager: IAdapterManager,
    private val adapterFactory: AdapterFactory
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _adaptersReadyFlow =
        MutableSharedFlow<Map<TransactionSource, ITransactionsAdapter>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val adaptersReadyFlow get() = _adaptersReadyFlow.asSharedFlow()

    val adaptersMap = ConcurrentHashMap<TransactionSource, ITransactionsAdapter>()

    init {
        coroutineScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect(::initAdapters)
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
                    BlockchainType.Base,
                    BlockchainType.ZkSync,
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
                    BlockchainType.Ton -> {
                        adapterFactory.tonTransactionsAdapter(wallet.transactionSource)
                    }
                    BlockchainType.Stellar -> {
                        adapterFactory.stellarTransactionsAdapter(wallet.transactionSource)
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

        _adaptersReadyFlow.tryEmit(this.adaptersMap)
    }
}

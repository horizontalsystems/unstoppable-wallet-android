package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
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
    private val adapterFactory: AdapterFactory,
    private val restoreSettingsManager: RestoreSettingsManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _adaptersReadyFlow =
        MutableSharedFlow<Map<TransactionSource, ITransactionsAdapter>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val adaptersReadyFlow get() = _adaptersReadyFlow.asSharedFlow()

    private val _adaptersInvalidatedFlow = MutableSharedFlow<BlockchainType>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val adaptersInvalidatedFlow get() = _adaptersInvalidatedFlow.asSharedFlow()

    val adaptersMap = ConcurrentHashMap<TransactionSource, ITransactionsAdapter>()

    private val pendingInvalidations = mutableSetOf<BlockchainType>()

    init {
        coroutineScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect(::initAdapters)
        }
        coroutineScope.launch {
            restoreSettingsManager.settingsUpdatedFlow.collect { blockchainType ->
                invalidateAdapters(blockchainType)
            }
        }
    }

    private fun invalidateAdapters(blockchainType: BlockchainType) {
        val sourcesToRemove = adaptersMap.keys.filter { it.blockchain.type == blockchainType }
        sourcesToRemove.forEach { source ->
            adaptersMap.remove(source)?.let {
                adapterFactory.unlinkAdapter(source)
            }
        }
        // Track pending invalidation - will emit after initAdapters sets up new adapter
        pendingInvalidations.add(blockchainType)
    }

    fun getAdapter(source: TransactionSource): ITransactionsAdapter? = adaptersMap[source]

    private fun initAdapters(adaptersMap: Map<Wallet, IAdapter>) {
        val currentAdapters = this.adaptersMap.toMutableMap()
        this.adaptersMap.clear()

        for ((wallet, adapter) in adaptersMap) {
            val source = wallet.transactionSource
            if (this.adaptersMap.containsKey(source)) continue

            val blockchainType = source.blockchain.type

            // For blockchains that use ITransactionsAdapter directly from the adapter (like Monero, Zcash),
            // always use the fresh adapter instance to handle cases where the adapter was recreated
            // (e.g., after birthday height change)
            val txAdapter = when (blockchainType) {
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
                    currentAdapters.remove(source)
                        ?: adapterFactory.evmTransactionsAdapter(wallet.transactionSource, blockchainType)
                }
                BlockchainType.Solana -> {
                    currentAdapters.remove(source)
                        ?: adapterFactory.solanaTransactionsAdapter(wallet.transactionSource)
                }
                BlockchainType.Tron -> {
                    currentAdapters.remove(source)
                        ?: adapterFactory.tronTransactionsAdapter(wallet.transactionSource)
                }
                BlockchainType.Ton -> {
                    currentAdapters.remove(source)
                        ?: adapterFactory.tonTransactionsAdapter(wallet.transactionSource)
                }
                BlockchainType.Stellar -> {
                    currentAdapters.remove(source)
                        ?: adapterFactory.stellarTransactionsAdapter(wallet.transactionSource)
                }
                else -> {
                    // For Monero, Zcash, Bitcoin, etc. - always use fresh adapter from AdapterManager
                    currentAdapters.remove(source)
                    adapter as? ITransactionsAdapter
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

        // Emit pending invalidations now that new adapters are ready
        pendingInvalidations.forEach { blockchainType ->
            _adaptersInvalidatedFlow.tryEmit(blockchainType)
        }
        pendingInvalidations.clear()
    }
}

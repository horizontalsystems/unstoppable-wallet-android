package cash.p.terminal.core.managers

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransactionAdapterManager(
    private val adapterManager: IAdapterManager,
    private val adapterFactory: AdapterFactory,
    dispatcherProvider: DispatcherProvider
) {
    private data class AdapterEntry(
        val walletAdapterRefs: Set<AdapterRef>,
        val transactionsAdapter: ITransactionsAdapter
    )

    private class AdapterRef(val adapter: IAdapter) {
        override fun equals(other: Any?): Boolean =
            other is AdapterRef && adapter === other.adapter

        override fun hashCode(): Int =
            System.identityHashCode(adapter)
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val operationsMutex = Mutex()
    private var adapterEntries: Map<TransactionSource, AdapterEntry> = emptyMap()

    private val _adaptersState =
        MutableStateFlow<Map<TransactionSource, ITransactionsAdapter>>(emptyMap())
    val adaptersReadyFlow = _adaptersState.asStateFlow()

    private val _initializationFlow = MutableStateFlow(false)
    val initializationFlow = _initializationFlow.asStateFlow()

    init {
        coroutineScope.launch {
            adapterManager.initializationInProgressFlow.collect { inProgress ->
                if (inProgress) {
                    _initializationFlow.value = false
                } else {
                    // Wait for any in-progress initAdapters to finish before
                    // signaling initialization complete. This guarantees
                    // _adaptersState has the final adapter map.
                    operationsMutex.withLock {
                        _initializationFlow.value = true
                    }
                }
            }
        }

        coroutineScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect(::initAdapters)
        }
    }

    fun getAdapter(source: TransactionSource): ITransactionsAdapter? =
        _adaptersState.value[source]

    fun close() {
        coroutineScope.cancel()
    }

    private suspend fun initAdapters(adaptersMap: Map<Wallet, IAdapter>) =
        operationsMutex.withLock {
            val currentEntries = adapterEntries
            val newEntries = mutableMapOf<TransactionSource, AdapterEntry>()
            val reusedSources = mutableSetOf<TransactionSource>()

            for ((source, entries) in adaptersMap.entries.groupBy { it.key.transactionSource }) {
                val walletAdapterRefs = entries.mapTo(linkedSetOf()) { AdapterRef(it.value) }
                val adapter = entries.first().value

                val entry = currentEntries[source]
                    ?.takeIf { it.walletAdapterRefs == walletAdapterRefs }
                    ?.also { reusedSources += source }
                    ?: createTransactionAdapter(adapter, source)?.let { txAdapter ->
                        AdapterEntry(walletAdapterRefs, txAdapter)
                    }

                entry?.let {
                    newEntries[source] = it
                }
            }

            val sourcesToUnlink = currentEntries.keys - reusedSources
            sourcesToUnlink.forEach { source ->
                adapterFactory.unlinkAdapter(source)
            }

            adapterEntries = newEntries
            _adaptersState.value = newEntries.mapValues { it.value.transactionsAdapter }
        }

    private suspend fun createTransactionAdapter(
        adapter: IAdapter,
        source: TransactionSource
    ): ITransactionsAdapter? {
        return when (val blockchainType = source.blockchain.type) {
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
                adapterFactory.evmTransactionsAdapter(source, blockchainType)
            }

            BlockchainType.Solana -> {
                adapterFactory.solanaTransactionsAdapter(source)
            }

            BlockchainType.Tron -> {
                adapterFactory.tronTransactionsAdapter(source)
            }

            BlockchainType.Ton -> {
                adapterFactory.tonTransactionsAdapter(source)
            }

            BlockchainType.Monero -> {
                adapterFactory.moneroTransactionsAdapter(source)
            }

            BlockchainType.Stellar -> {
                adapterFactory.stellarTransactionsAdapter(source)
            }

            else -> adapter as? ITransactionsAdapter
        }
    }
}

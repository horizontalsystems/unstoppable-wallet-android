package cash.p.terminal.core.managers

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransactionAdapterManager(
    private val adapterManager: IAdapterManager,
    private val adapterFactory: AdapterFactory
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val operationsMutex = Mutex()

    private val _adaptersState =
        MutableStateFlow<Map<TransactionSource, ITransactionsAdapter>>(emptyMap())
    val adaptersReadyFlow = _adaptersState.asStateFlow()

    private val _initializationFlow = MutableStateFlow<Boolean>(false)
    val initializationFlow = _initializationFlow.asStateFlow()

    init {
        coroutineScope.launch {
            adapterManager.initializationInProgressFlow.filter { it }.collect {
                _initializationFlow.value = false
            }
        }

        coroutineScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect(::initAdapters)
        }
    }

    fun getAdapter(source: TransactionSource): ITransactionsAdapter? =
        _adaptersState.value[source]

    private suspend fun initAdapters(adaptersMap: Map<Wallet, IAdapter>) =
        operationsMutex.withLock {
            _initializationFlow.value = false

            val currentAdapters = _adaptersState.value
            val newAdapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()

            for ((wallet, adapter) in adaptersMap) {
                val source = wallet.transactionSource
                if (newAdapters.containsKey(source)) continue

                val existingAdapter = currentAdapters[source]
                val txAdapter = existingAdapter ?: createTransactionAdapter(adapter, source)

                txAdapter?.let {
                    newAdapters[source] = it
                }
            }

            currentAdapters.forEach {
                adapterFactory.unlinkAdapter(it.key)
            }

            _adaptersState.value = newAdapters
            _initializationFlow.value = true
        }

    private fun createTransactionAdapter(
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

            else -> adapter as? ITransactionsAdapter
        }
    }
}
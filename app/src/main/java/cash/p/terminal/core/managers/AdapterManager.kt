package cash.p.terminal.core.managers

import android.os.HandlerThread
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.FallbackAddressProvider
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.core.entities.BlockchainType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
    private val walletManager: IWalletManager,
    private val adapterFactory: AdapterFactory,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager,
    private val moneroKitManager: MoneroKitManager,
    private val stellarKitManager: StellarKitManager,
    private val pendingBalanceCalculator: PendingBalanceCalculator,
    private val fallbackAddressProvider: FallbackAddressProvider
) : IAdapterManager, HandlerThread("A") {

    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    private val _initializationInProgressFlow = MutableStateFlow(true)
    override val initializationInProgressFlow = _initializationInProgressFlow.asStateFlow()

    private val _walletBalanceUpdatedFlow = MutableSharedFlow<Wallet>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val walletBalanceUpdatedFlow: SharedFlow<Wallet> = _walletBalanceUpdatedFlow.asSharedFlow()

    private val balanceSubscriptionJobs = ConcurrentHashMap<Wallet, Job>()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> =
        adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    private val initRequests = MutableSharedFlow<List<Wallet>>(extraBufferCapacity = 1)

    init {
        start()

        coroutineScope.launch {
            initRequests
                .conflate()
                .collectLatest { wallets ->
                    mutex.withLock {
                        initAdaptersInternal(wallets)
                    }
                }
        }
    }

    override fun startAdapterManager() {
        coroutineScope.launch {
            walletManager.activeWalletsFlow.collect { wallets ->
                requestInitAdapters(wallets)
            }
        }
        coroutineScope.launch {
            btcBlockchainManager.restoreModeUpdatedObservable.asFlow().collect {
                reinitAdapters(it)
            }
        }
        coroutineScope.launch {
            solanaKitManager.kitStoppedObservable.asFlow().collect {
                reinitAdapters(BlockchainType.Solana)
            }
        }
        coroutineScope.launch {
            moneroKitManager.kitStoppedObservable.asFlow().collect {
                reinitAdapters(BlockchainType.Monero)
            }
        }
        for (blockchain in evmBlockchainManager.allBlockchains) {
            coroutineScope.launch {
                evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitUpdatedObservable.asFlow()
                    .collect {
                        reinitAdapters(blockchain.type)
                    }
            }
        }
    }

    private fun reinitAdapters(blockchainType: BlockchainType) {
        val wallets = adaptersMap.keys.filter { it.token.blockchainType == blockchainType }
        if (wallets.isEmpty()) return

        wallets.forEach { wallet ->
            balanceSubscriptionJobs.remove(wallet)?.cancel()
            adaptersMap[wallet]?.stop()
            adaptersMap.remove(wallet)
        }

        requestInitAdapters(walletManager.activeWallets)
    }

    override suspend fun refresh() {
        coroutineScope.launch {
            adaptersMap.values.forEach { it.refresh() }
        }

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).refresh()
        }

        solanaKitManager.solanaKitWrapper?.solanaKit?.refresh()
        tronKitManager.tronKitWrapper?.tronKit?.refresh()
        tonKitManager.tonKitWrapper?.tonKit?.refresh()
        moneroKitManager.moneroKitWrapper?.refresh()
        stellarKitManager.stellarKitWrapper?.stellarKit?.refresh()
    }

    private fun requestInitAdapters(wallets: List<Wallet>) {
        initRequests.tryEmit(wallets)
    }

    private suspend fun initAdaptersInternal(wallets: List<Wallet>) {
        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()
        _initializationInProgressFlow.value = true

        // Only one account is active at a time
        val activeAccountId = wallets.firstOrNull()?.account?.id
        val previousAccountId = currentAdapters.keys.firstOrNull()?.account?.id

        if (activeAccountId != null) {
            pendingBalanceCalculator.startObserving(activeAccountId)
        }

        // Separate reusable adapters from ones that need creation
        val reusable = mutableMapOf<Wallet, IAdapter>()
        val toCreate = mutableListOf<Wallet>()

        wallets.forEach { wallet ->
            val existing = currentAdapters.remove(wallet)
            if (existing != null) {
                reusable[wallet] = existing
            } else {
                toCreate.add(wallet)
            }
        }

        // Stop old adapters that won't be reused BEFORE creating new ones.
        // This is critical for Zcash: its SDK forbids creating a new Synchronizer
        // while another one with the same alias is still active.
        currentAdapters.forEach { (wallet, adapter) ->
            balanceSubscriptionJobs.remove(wallet)?.cancel()
            adapter.stop()
            coroutineScope.launch {
                adapterFactory.unlinkAdapter(wallet)
            }
        }

        // Add reusable adapters immediately and subscribe to balance updates
        adaptersMap.putAll(reusable)
        reusable.forEach { (wallet, adapter) ->
            (adapter as? IBalanceAdapter)?.let { subscribeToBalanceUpdates(wallet, it) }
        }

        // Emit immediately so transaction loading can start with reusable adapters
        if (reusable.isNotEmpty()) {
            adaptersReadySubject.onNext(HashMap(adaptersMap))
        }

        // Create new adapters in parallel with two-phase emission:
        // Phase 1: early batch after EARLY_BATCH_DELAY_MS — captures fast adapters
        // Phase 2: final emission after all adapters complete
        if (toCreate.isNotEmpty()) {
            supervisorScope {
                val jobs = toCreate.map { wallet ->
                    launch {
                        try {
                            val adapter = adapterFactory.getAdapterOrNull(wallet)
                            adapter?.start()
                            adapter?.let {
                                adaptersMap[wallet] = it
                                (it as? IBalanceAdapter)?.let { ba -> subscribeToBalanceUpdates(wallet, ba) }
                            }
                        } catch (ex: Exception) {
                            Timber.e(ex, "Can't get adapter")
                        }
                    }
                }

                // Early batch: emit whatever is ready after a short delay
                val earlyBatchJob = launch {
                    delay(EARLY_BATCH_DELAY_MS)
                    if (jobs.any { it.isActive }) {
                        adaptersReadySubject.onNext(HashMap(adaptersMap))
                    }
                }

                jobs.joinAll()
                earlyBatchJob.cancel()
            }

            // Final emission with all adapters
            adaptersReadySubject.onNext(HashMap(adaptersMap))
        }

        // Stop observing if account changed
        if (previousAccountId != null && previousAccountId != activeAccountId) {
            pendingBalanceCalculator.stopObserving(previousAccountId)
        }
        _initializationInProgressFlow.value = false
    }

    /**
     * Partial refresh of adapters
     * For the given list of wallets do:
     * - remove corresponding adapters from adaptersMap and stop them
     * - create new adapters, start and add them to adaptersMap
     * - trigger adaptersReadySubject
     */
    override fun refreshAdapters(wallets: List<Wallet>) {
        coroutineScope.launch {
            mutex.withLock {
                val walletsToRefresh = wallets.filter { adaptersMap.containsKey(it) }

                // remove and stop adapters
                walletsToRefresh.forEach { wallet ->
                    balanceSubscriptionJobs.remove(wallet)?.cancel()
                    adaptersMap.remove(wallet)?.let { previousAdapter ->
                        previousAdapter.stop()
                        coroutineScope.launch {
                            adapterFactory.unlinkAdapter(wallet)
                        }
                    }
                }

                // add and start new adapters
                walletsToRefresh.forEach { wallet ->
                    adapterFactory.getAdapterOrNull(wallet)?.let { adapter ->
                        adaptersMap[wallet] = adapter
                        adapter.start()
                        (adapter as? IBalanceAdapter)?.let { balanceAdapter ->
                            subscribeToBalanceUpdates(wallet, balanceAdapter)
                        }
                    }
                }

                adaptersReadySubject.onNext(HashMap(adaptersMap))
            }
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.token)

        if (blockchain != null) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        } else {
            coroutineScope.launch {
                adaptersMap[wallet]?.refresh()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> awaitAdapterForWallet(wallet: Wallet, timeoutMs: Long): T? {
        (adaptersMap[wallet] as? T)?.let { return it }

        return withTimeoutOrNull(timeoutMs) {
            merge(
                initializationInProgressFlow.filter { !it }.map { adaptersMap },
                adaptersReadyObservable.asFlow()
            )
                .mapNotNull { it[wallet] as? T }
                .first()
        } ?: adaptersMap[wallet] as? T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getAdapterForWallet(wallet: Wallet): T? {
        return adaptersMap[wallet] as? T
    }

    override fun getAdapterForWalletOld(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getAdapterForToken(token: Token): T? {
        return walletManager.activeWallets.firstOrNull { it.token == token }
            ?.let(::getAdapterForWallet)
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

    override fun getAdjustedBalanceData(wallet: Wallet): BalanceData? {
        val adapter = getBalanceAdapterForWallet(wallet) ?: return null
        return pendingBalanceCalculator.adjustBalance(wallet, adapter.balanceData)
    }

    override fun getAdjustedBalanceDataForToken(token: Token): BalanceData? {
        val wallet = walletManager.activeWallets.firstOrNull { it.token == token } ?: return null
        return getAdjustedBalanceData(wallet)
    }

    override suspend fun getReceiveAddressForWallet(wallet: Wallet): String? {
        getReceiveAdapterForWallet(wallet)?.receiveAddress?.let { return it }
        return fallbackAddressProvider.getAddress(wallet)
    }

    private fun subscribeToBalanceUpdates(wallet: Wallet, adapter: IBalanceAdapter) {
        balanceSubscriptionJobs[wallet]?.cancel()
        balanceSubscriptionJobs[wallet] = coroutineScope.launch {
            merge(
                adapter.balanceUpdatedFlow,
                adapter.balanceStateUpdatedFlow
            ).collectLatest {
                _walletBalanceUpdatedFlow.emit(wallet)
            }
        }
    }

    companion object {
        private const val EARLY_BATCH_DELAY_MS = 2000L
    }
}

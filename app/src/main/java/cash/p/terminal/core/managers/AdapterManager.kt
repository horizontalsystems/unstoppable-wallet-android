package cash.p.terminal.core.managers

import android.os.HandlerThread
import cash.p.terminal.core.factories.AdapterFactory
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
    private val walletManager: IWalletManager,
    private val adapterFactory: AdapterFactory,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val solanaKitManager: SolanaKitManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager,
    private val moneroKitManager: MoneroKitManager
) : IAdapterManager, HandlerThread("A") {

    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    private val _initializationInProgressFlow = MutableStateFlow<Boolean>(true)
    override val initializationInProgressFlow = _initializationInProgressFlow.asStateFlow()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> =
        adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        start()
    }

    override fun startAdapterManager() {
        coroutineScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect { wallets ->
                initAdapters(wallets)
            }
        }
        coroutineScope.launch {
            btcBlockchainManager.restoreModeUpdatedObservable.asFlow().collect {
                handleUpdatedRestoreMode(it)
            }
        }
        coroutineScope.launch {
            solanaKitManager.kitStoppedObservable.asFlow().collect {
                handleUpdatedKit(BlockchainType.Solana)
            }
        }
        coroutineScope.launch {
            moneroKitManager.kitStoppedObservable.asFlow().collect {
                handleUpdatedKit(BlockchainType.Monero)
            }
        }
        for (blockchain in evmBlockchainManager.allBlockchains) {
            coroutineScope.launch {
                evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitUpdatedObservable.asFlow()
                    .collect {
                        handleUpdatedKit(blockchain.type)
                    }
            }
        }
    }

    private fun handleUpdatedKit(blockchainType: BlockchainType) {
        val wallets = adaptersMap.keys().toList().filter {
            it.token.blockchain.type == blockchainType
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    private fun handleUpdatedRestoreMode(blockchainType: BlockchainType) {
        val wallets = adaptersMap.keys().toList().filter {
            it.token.blockchainType == blockchainType
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    override suspend fun refresh() {
        coroutineScope.launch {
            adaptersMap.values.forEach { it.refresh() }
        }

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        }

        solanaKitManager.solanaKitWrapper?.solanaKit?.refresh()
        tronKitManager.tronKitWrapper?.tronKit?.refresh()
        tonKitManager.tonKitWrapper?.tonKit?.refresh()
        moneroKitManager.moneroKitWrapper?.refresh()
    }

    private fun initAdapters(wallets: List<Wallet>) = coroutineScope.launch {
        mutex.withLock {
            val currentAdapters = adaptersMap.toMutableMap()
            adaptersMap.clear()
            _initializationInProgressFlow.value = true

            wallets.forEach { wallet ->
                var adapter = currentAdapters.remove(wallet)
                if (adapter == null) {
                    adapterFactory.getAdapterOrNull(wallet)?.let {
                        it.start()

                        adapter = it
                    }
                }

                adapter?.let {
                    adaptersMap[wallet] = it
                }
            }

            adaptersReadySubject.onNext(adaptersMap)

            currentAdapters.forEach { (wallet, adapter) ->
                adapter.stop()
                coroutineScope.launch {
                    adapterFactory.unlinkAdapter(wallet)
                }
            }
            _initializationInProgressFlow.value = false
        }
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

                //remove and stop adapters
                walletsToRefresh.forEach { wallet ->
                    adaptersMap.remove(wallet)?.let { previousAdapter ->
                        previousAdapter.stop()
                        coroutineScope.launch {
                            adapterFactory.unlinkAdapter(wallet)
                        }
                    }
                }

                //add and start new adapters
                walletsToRefresh.forEach { wallet ->
                    adapterFactory.getAdapterOrNull(wallet)?.let { adapter ->
                        adaptersMap[wallet] = adapter
                        adapter.start()
                    }
                }

                adaptersReadySubject.onNext(adaptersMap)
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

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForToken(token: Token): IAdapter? {
        return walletManager.activeWallets.firstOrNull { it.token == token }
            ?.let { wallet ->
                adaptersMap[wallet]
            }
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

}

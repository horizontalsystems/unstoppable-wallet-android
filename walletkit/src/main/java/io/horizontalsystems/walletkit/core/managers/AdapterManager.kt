package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.factories.AdapterFactory
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
    private val walletManager: WalletManager,
    private val adapterFactory: AdapterFactory,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val tronKitManager: TronKitManager,
    private val tonKitManager: TonKitManager,
    private val stellarKitManager: StellarKitManager,
    private val thorchainKitManager: ThorchainKitManager,
    private val mayachainKitManager: ThorchainKitManager,
) : IAdapterManager {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()

    @Volatile
    private var adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> =
        adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun startAdapterManager() {
        coroutineScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect { wallets ->
                initAdapters(wallets)
            }
        }
    }

    override suspend fun refresh() {
        adaptersMap.values.forEach { it.refresh() }

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        }

        ChainRegistry.all.forEach { it.refreshKit() }
        tronKitManager.tronKitWrapper?.tronKit?.refresh()
        tonKitManager.tonKitWrapper?.tonKit?.refresh()
        stellarKitManager.stellarKitWrapper?.stellarKit?.refresh()
        thorchainKitManager.thorchainKitWrapper?.thorchainKit?.refresh()
        mayachainKitManager.thorchainKitWrapper?.thorchainKit?.refresh()
    }

    @Synchronized
    private fun initAdapters(wallets: List<Wallet>) {
        val currentAdapters = adaptersMap.toMutableMap()

        // Build into a new map and swap it in atomically. Clearing and refilling the
        // live map exposes a window where enabled wallets briefly have no adapter;
        // the balance module subscribing to adapter updates during that window
        // silently skips those wallets and their rows freeze on a stale sync state.
        val newAdaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

        wallets.forEach { wallet ->
            var adapter = currentAdapters.remove(wallet)
            if (adapter == null) {
                adapterFactory.getAdapterOrNull(wallet)?.let {
                    it.start()

                    adapter = it
                }
            }

            adapter?.let {
                newAdaptersMap[wallet] = it
            }
        }

        adaptersMap = newAdaptersMap

        adaptersReadySubject.onNext(adaptersMap)

        currentAdapters.forEach { (wallet, adapter) ->
            adapter.stop()
            adapterFactory.unlinkAdapter(wallet)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.token)

        if (blockchain != null) {
            evmBlockchainManager.getEvmKitManager(blockchain.type).evmKitWrapper?.evmKit?.refresh()
        } else {
            adaptersMap[wallet]?.refresh()
        }
    }

    override fun <T> getAdapterForToken(token: Token): T? {
        return walletManager.activeWallets.firstOrNull { it.token == token }
            ?.let { getAdapterForWallet(it) }
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getAdapterForWallet(wallet: Wallet): T? {
        return adaptersMap[wallet] as? T
    }
}

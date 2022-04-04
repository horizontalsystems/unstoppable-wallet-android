package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
    private val walletManager: IWalletManager,
    private val adapterFactory: AdapterFactory,
    btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val binanceKitManager: BinanceKitManager,
) : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()
    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> =
        adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        start()
        handler = Handler(looper)

        disposables.add(walletManager.activeWalletsUpdatedObservable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { wallets ->
                initAdapters(wallets)
            }
        )

        disposables.add(btcBlockchainManager.restoreModeUpdatedObservable
            .subscribeIO {
                handleUpdatedRestoreMode(it)
            }
        )

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain).evmKitUpdatedObservable
                .subscribeIO {
                    handleUpdatedKit(blockchain)
                }
                .let {
                    disposables.add(it)
                }
        }
    }

    private fun handleUpdatedKit(blockchain: EvmBlockchain) {
        val wallets = adaptersMap.keys().toList().filter {
            blockchain.supports(it.coinType)
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    private fun handleUpdatedRestoreMode(blockchain: BtcBlockchain) {
        val wallets = adaptersMap.keys().toList().filter {
            blockchain.supports(it.coinType)
        }

        if (wallets.isEmpty()) return

        wallets.forEach {
            adaptersMap[it]?.stop()
            adaptersMap.remove(it)
        }

        initAdapters(walletManager.activeWallets)
    }

    override fun preloadAdapters() {
        handler.post {
            initAdapters(walletManager.activeWallets)
        }
    }

    override fun refresh() {
        handler.post {
            adaptersMap.values.forEach { it.refresh() }
        }

        for (blockchain in evmBlockchainManager.allBlockchains) {
            evmBlockchainManager.getEvmKitManager(blockchain).evmKitWrapper?.evmKit?.refresh()
        }

        binanceKitManager.binanceKit?.refresh()
    }

    @Synchronized
    private fun initAdapters(wallets: List<Wallet>) {
        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()

        wallets.forEach { wallet ->
            var adapter = currentAdapters.remove(wallet)
            if (adapter == null) {
                adapterFactory.getAdapter(wallet)?.let {
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
            adapterFactory.unlinkAdapter(wallet)
        }
    }

    /**
     * Partial refresh of adapters
     * For the given list of wallets do:
     * - remove corresponding adapters from adaptersMap and stop them
     * - create new adapters, start and add them to adaptersMap
     * - trigger adaptersReadySubject
     */
    @Synchronized
    override fun refreshAdapters(wallets: List<Wallet>) {
        handler.post {
            val walletsToRefresh = wallets.filter { adaptersMap.containsKey(it) }

            //remove and stop adapters
            walletsToRefresh.forEach { wallet ->
                adaptersMap.remove(wallet)?.let { previousAdapter ->
                    previousAdapter.stop()
                    adapterFactory.unlinkAdapter(wallet)
                }
            }

            //add and start new adapters
            walletsToRefresh.forEach { wallet ->
                adapterFactory.getAdapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter
                    adapter.start()
                }
            }

            adaptersReadySubject.onNext(adaptersMap)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val blockchain = evmBlockchainManager.getBlockchain(wallet.coinType)

        if (blockchain != null) {
            evmBlockchainManager.getEvmKitManager(blockchain).evmKitWrapper?.evmKit?.refresh()
        } else {
            adaptersMap[wallet]?.refresh()
        }
    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForPlatformCoin(platformCoin: PlatformCoin): IAdapter? {
        return walletManager.activeWallets.firstOrNull { it.platformCoin == platformCoin }
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

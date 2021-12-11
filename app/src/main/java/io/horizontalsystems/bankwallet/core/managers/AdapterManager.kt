package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.BinanceAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinType
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
        private val ethereumKitManager: EvmKitManager,
        private val binanceSmartChainKitManager: EvmKitManager,
        private val binanceKitManager: BinanceKitManager
) : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()
    private val adaptersReadySubject = PublishSubject.create<Map<Wallet, IAdapter>>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    override val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>> = adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

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

        ethereumKitManager.evmKitUpdatedObservable
            .subscribeIO {
                handleUpdatedEthereumKit()
            }
            .let {
                disposables.add(it)
            }

        binanceSmartChainKitManager.evmKitUpdatedObservable
            .subscribeIO {
                handleUpdatedBinanceSmartChainKit()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedEthereumKit() {
        handleUpdatedKit {
            it.coinType is CoinType.Ethereum || it.coinType is CoinType.Erc20
        }
    }

    private fun handleUpdatedBinanceSmartChainKit() {
        handleUpdatedKit {
            it.coinType is CoinType.BinanceSmartChain || it.coinType is CoinType.Bep20
        }
    }

    private fun handleUpdatedKit(filter: (Wallet) -> Boolean) {
        val wallets = adaptersMap.keys().toList().filter(filter)

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

        ethereumKitManager.evmKit?.refresh()
        binanceSmartChainKitManager.evmKit?.refresh()
        binanceKitManager.binanceKit?.refresh()
    }

    @Synchronized
    private fun initAdapters(wallets: List<Wallet>) {
        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()

        wallets.forEach { wallet ->
            var adapter = currentAdapters.remove(wallet)
            if (adapter == null) {
                adapterFactory.adapter(wallet)?.let {
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
                adapterFactory.adapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter
                    adapter.start()
                }
            }

            adaptersReadySubject.onNext(adaptersMap)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val adapter = adaptersMap[wallet] ?: return

        when (adapter) {
            is BinanceAdapter -> binanceKitManager.binanceKit?.refresh()
            is BaseEvmAdapter -> {
                when (wallet.coinType) {
                    CoinType.Ethereum, is CoinType.Erc20 -> ethereumKitManager.evmKit?.refresh()
                    CoinType.BinanceSmartChain, is CoinType.Bep20 -> binanceSmartChainKitManager.evmKit?.refresh()
                }
            }
            else -> adapter.refresh()
        }

    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForPlatformCoin(platformCoin: PlatformCoin): IAdapter? {
        return walletManager.activeWallets.firstOrNull { it.platformCoin == platformCoin }?.let { wallet ->
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

package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.BinanceAdapter
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap

class AdapterManager(
        private val walletManager: IWalletManager,
        private val adapterFactory: AdapterFactory,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager,
        private val binanceKitManager: BinanceKitManager
) : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()
    private val adaptersReadySubject = PublishSubject.create<Unit>()
    private val adaptersMap = ConcurrentHashMap<Wallet, IAdapter>()

    override val adaptersReadyObservable: Flowable<Unit> = adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        start()
        handler = Handler(looper)

        disposables.add(walletManager.walletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { wallets ->
                    initAdapters(wallets)
                }
        )
    }

    override fun preloadAdapters() {
        handler.post {
            initAdapters(walletManager.wallets)
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
        val disabledWallets = adaptersMap.keys.subtract(wallets)

        wallets.forEach { wallet ->
            if (!adaptersMap.containsKey(wallet)) {
                adapterFactory.adapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter

                    adapter.start()
                }
            }
        }

        adaptersReadySubject.onNext(Unit)

        disabledWallets.forEach { wallet ->
            adaptersMap.remove(wallet)?.let { disabledAdapter ->
                disabledAdapter.stop()
                adapterFactory.unlinkAdapter(wallet.coin.type)
            }
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
                    adapterFactory.unlinkAdapter(wallet.coin.type)
                }
            }

            //add and start new adapters
            walletsToRefresh.forEach { wallet ->
                adapterFactory.adapter(wallet)?.let { adapter ->
                    adaptersMap[wallet] = adapter
                    adapter.start()
                }
            }

            adaptersReadySubject.onNext(Unit)
        }
    }

    override fun refreshByWallet(wallet: Wallet) {
        val adapter = adaptersMap[wallet] ?: return

        when (adapter) {
            is BinanceAdapter -> binanceKitManager.binanceKit?.refresh()
            is BaseEvmAdapter -> {
                when (wallet.coin.type) {
                    CoinType.Ethereum, is CoinType.Erc20 -> ethereumKitManager.evmKit?.refresh()
                    CoinType.BinanceSmartChain, is CoinType.Bep20 -> binanceSmartChainKitManager.evmKit?.refresh()
                }
            }
            else -> adapter.refresh()
        }

    }

    override fun stopKits() {
        handler.post {
            adaptersMap.forEach { (wallet, adapter) ->
                adapter.stop()
                adapterFactory.unlinkAdapter(wallet.coin.type)
            }
            adaptersMap.clear()
        }
    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getAdapterForCoin(coin: Coin): IAdapter? {
        return walletManager.wallets.firstOrNull { it.coin == coin }?.let { wallet ->
            adaptersMap[wallet]
        }
    }

    override fun getTransactionsAdapterForWallet(wallet: Wallet): ITransactionsAdapter? {
        return adaptersMap[wallet]?.let { it as? ITransactionsAdapter }
    }

    override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? {
        return adaptersMap[wallet]?.let { it as? IBalanceAdapter }
    }

    override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? {
        return adaptersMap[wallet]?.let { it as? IReceiveAdapter }
    }

}
